package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.ReadOnlyBufferException;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.keybind.KeymapEntry;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.window.Frame;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * overriding keymap設定時に保持する状態。
 */
record OverridingKeymapState(Keymap keymap, Runnable onUnboundKeyExit) {}

/**
 * キー入力→コマンド解決→コマンド実行のメインループ。
 * InputSourceからキーを読み、KeyResolverでコマンドを解決し、実行する。
 * コマンドはCompletableFutureを返し、後続処理はthenRunで繋ぐ（fire-and-forget）。
 * キーマップに未マッチのキーは無視される。
 *
 * processKeyは1キーにつき1回の状態遷移で即座にreturnする（非ブロッキング）。
 * プレフィックスキーの場合は内部状態として保持し、次のprocessKey呼び出しで解決する。
 */
public class CommandLoop {

    private static final Logger logger = Logger.getLogger(CommandLoop.class.getName());

    private final InputSource inputSource;
    private final KeyResolver keyResolver;
    private final Frame frame;
    private final BufferManager bufferManager;
    private final InputPrompter inputPrompter;
    private final KillRing killRing;
    private final MessageBuffer messageBuffer;
    private final MessageBuffer warningBuffer;
    private final SettingsRegistry settingsRegistry;
    private final CommandResolver commandResolver;
    private Optional<String> lastCommand = Optional.empty();
    private @Nullable PendingPrefix pendingPrefix;
    private @Nullable OverridingKeymapState overridingKeymapState;

    private final OverridingKeymapController overridingKeymapController = new OverridingKeymapController() {
        @Override
        public void set(Keymap keymap, Runnable onUnboundKeyExit) {
            overridingKeymapState = new OverridingKeymapState(keymap, onUnboundKeyExit);
        }

        @Override
        public void clear() {
            overridingKeymapState = null;
        }
    };

    /**
     * プレフィックスキー入力待ち状態を表す。
     */
    private record PendingPrefix(Keymap keymap, String displayText) {}

    public CommandLoop(
            InputSource inputSource,
            KeyResolver keyResolver,
            Frame frame,
            BufferManager bufferManager,
            InputPrompter inputPrompter,
            KillRing killRing,
            MessageBuffer messageBuffer,
            MessageBuffer warningBuffer,
            SettingsRegistry settingsRegistry,
            CommandResolver commandResolver) {
        this.inputSource = inputSource;
        this.keyResolver = keyResolver;
        this.frame = frame;
        this.bufferManager = bufferManager;
        this.inputPrompter = inputPrompter;
        this.killRing = killRing;
        this.messageBuffer = messageBuffer;
        this.warningBuffer = warningBuffer;
        this.settingsRegistry = settingsRegistry;
        this.commandResolver = commandResolver;
    }

    /**
     * メインループを実行する。
     * InputSourceがemptyを返すと終了する。
     */
    public void run() {
        while (true) {
            var keyOpt = inputSource.readKeyStroke();
            if (keyOpt.isEmpty()) {
                break;
            }
            processKey(keyOpt.get());
        }
    }

    /**
     * 1つのキーストロークを処理する。テスト用に公開。
     * 非ブロッキング: 1キーにつき1回の状態遷移で即座にreturnする。
     * プレフィックスキーの場合は内部状態として保持し、次の呼び出しで解決する。
     */
    public void processKey(KeyStroke keyStroke) {
        // overriding keymap有効時はエコー表示を維持する
        if (overridingKeymapState == null) {
            messageBuffer.clearShowingMessage();
        }

        if (pendingPrefix != null) {
            processPrefixKey(keyStroke);
        } else {
            processNormalKey(keyStroke);
        }
    }

    private void processNormalKey(KeyStroke keyStroke) {
        // overriding keymapが設定されている場合、最優先で解決
        if (overridingKeymapState != null) {
            var entry = overridingKeymapState.keymap().lookup(keyStroke);
            if (entry.isPresent()) {
                handleEntry(entry.get(), keyStroke, "");
                return;
            }
            // 未バインドキー: 終了コールバック → クリア → 通常解決にフォールスルー
            overridingKeymapState.onUnboundKeyExit().run();
            overridingKeymapState = null;
            messageBuffer.clearShowingMessage();
        }
        var entryOpt = resolveKey(keyStroke);
        if (entryOpt.isPresent()) {
            handleEntry(entryOpt.get(), keyStroke, "");
        }
    }

    private void processPrefixKey(KeyStroke keyStroke) {
        var prefix = pendingPrefix;
        pendingPrefix = null;
        if (prefix == null) {
            return;
        }

        var entryOpt = prefix.keymap().lookup(keyStroke);
        if (entryOpt.isPresent()) {
            handleEntry(entryOpt.get(), keyStroke, prefix.displayText());
        }
    }

    private Optional<KeymapEntry> resolveKey(KeyStroke keyStroke) {
        var buffer = frame.getActiveWindow().getBuffer();

        // 1. バッファローカルキーマップ（ミニバッファ用）
        var localKeymapOpt = buffer.getLocalKeymap();
        if (localKeymapOpt.isPresent()) {
            var localEntry = localKeymapOpt.get().lookup(keyStroke);
            if (localEntry.isPresent()) {
                return localEntry;
            }
        }

        // 2. マイナーモードキーマップ（後から有効にしたものが優先）
        var minorModes = buffer.getMinorModes();
        for (int i = minorModes.size() - 1; i >= 0; i--) {
            var modeKeymapOpt = minorModes.get(i).keymap();
            if (modeKeymapOpt.isPresent()) {
                var entry = modeKeymapOpt.get().lookup(keyStroke);
                if (entry.isPresent()) {
                    return entry;
                }
            }
        }

        // 3. メジャーモードキーマップ
        var majorKeymapOpt = buffer.getMajorMode().keymap();
        if (majorKeymapOpt.isPresent()) {
            var entry = majorKeymapOpt.get().lookup(keyStroke);
            if (entry.isPresent()) {
                return entry;
            }
        }

        // 4. グローバルキーマップ
        return keyResolver.resolve(keyStroke);
    }

    private void handleEntry(KeymapEntry entry, KeyStroke keyStroke, String prefixDisplay) {
        switch (entry) {
            case KeymapEntry.CommandBinding(var command) -> {
                var thisCommand = Optional.of(command.name());
                var context = new CommandContext(
                        frame,
                        bufferManager,
                        frame.getActiveWindow(),
                        inputPrompter,
                        Optional.of(keyStroke),
                        thisCommand,
                        lastCommand,
                        killRing,
                        messageBuffer,
                        warningBuffer,
                        settingsRegistry,
                        commandResolver,
                        overridingKeymapController);
                try {
                    var buffer = context.activeWindow().getBuffer();
                    buffer.getUndoManager().withTransaction(() -> {
                        command.execute(context)
                                .thenRun(() -> {
                                    lastCommand = thisCommand;
                                    if (!command.keepsRegionActive()) {
                                        context.activeWindow().clearMark();
                                    }
                                })
                                .exceptionally(ex -> {
                                    var cause = ex.getCause() != null ? ex.getCause() : ex;
                                    handleCommandError(command, context, cause);
                                    return null;
                                });
                    });
                } catch (ReadOnlyBufferException ex) {
                    messageBuffer.message("Text is read-only");
                } catch (Exception ex) {
                    handleCommandError(command, context, ex);
                }
            }
            case KeymapEntry.PrefixBinding(var prefixKeymap) -> {
                var displayText = prefixDisplay + keyStroke.displayString() + " ";
                pendingPrefix = new PendingPrefix(prefixKeymap, displayText);
                messageBuffer.message(displayText);
            }
        }
    }

    private void handleCommandError(Command command, CommandContext context, Throwable ex) {
        if (ex instanceof ReadOnlyBufferException) {
            messageBuffer.message("Text is read-only");
        } else {
            var message = "コマンド実行中にエラーが発生: " + command.name();
            logger.log(Level.WARNING, message, ex);
            context.handleError(message, ex);
        }
    }
}
