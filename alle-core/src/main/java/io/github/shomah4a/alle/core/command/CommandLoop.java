package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.keybind.KeymapEntry;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.WindowActor;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * キー入力→コマンド解決→コマンド実行のメインループ。
 * InputSourceからキーを読み、KeyResolverでコマンドを解決し、実行する。
 * コマンドはCompletableFutureを返し、後続処理はthenRunで繋ぐ（fire-and-forget）。
 * キーマップに未マッチのキーは無視される。
 */
public class CommandLoop {

    private static final Logger logger = Logger.getLogger(CommandLoop.class.getName());

    private final InputSource inputSource;
    private final KeyResolver keyResolver;
    private final Frame frame;
    private final BufferManager bufferManager;
    private final InputPrompter inputPrompter;
    private Optional<String> lastCommand = Optional.empty();

    public CommandLoop(
            InputSource inputSource,
            KeyResolver keyResolver,
            Frame frame,
            BufferManager bufferManager,
            InputPrompter inputPrompter) {
        this.inputSource = inputSource;
        this.keyResolver = keyResolver;
        this.frame = frame;
        this.bufferManager = bufferManager;
        this.inputPrompter = inputPrompter;
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
     * アクティブウィンドウのバッファにローカルキーマップがあればそちらを優先する。
     */
    public void processKey(KeyStroke keyStroke) {
        var entryOpt = resolveKey(keyStroke);
        if (entryOpt.isPresent()) {
            handleEntry(entryOpt.get(), keyStroke);
        }
    }

    private Optional<KeymapEntry> resolveKey(KeyStroke keyStroke) {
        var localKeymapOpt = frame.getActiveWindow().getBuffer().getLocalKeymap();
        if (localKeymapOpt.isPresent()) {
            var localEntry = localKeymapOpt.get().lookup(keyStroke);
            if (localEntry.isPresent()) {
                return localEntry;
            }
        }
        return keyResolver.resolve(keyStroke);
    }

    private void handleEntry(KeymapEntry entry, KeyStroke keyStroke) {
        switch (entry) {
            case KeymapEntry.CommandBinding(var command) -> {
                var thisCommand = Optional.of(command.name());
                var windowActor = new WindowActor(frame.getActiveWindow());
                var context = new CommandContext(
                        frame,
                        bufferManager,
                        windowActor,
                        inputPrompter,
                        Optional.of(keyStroke),
                        thisCommand,
                        lastCommand);
                command.execute(context)
                        .thenRun(() -> lastCommand = thisCommand)
                        .exceptionally(ex -> {
                            logger.log(Level.WARNING, "コマンド実行中に例外が発生: " + command.name(), ex);
                            return null;
                        });
            }
            case KeymapEntry.PrefixBinding(var prefixKeymap) -> handlePrefix(prefixKeymap);
        }
    }

    private void handlePrefix(Keymap prefixKeymap) {
        var nextKeyOpt = inputSource.readKeyStroke();
        if (nextKeyOpt.isEmpty()) {
            return;
        }
        var nextKey = nextKeyOpt.get();
        var entryOpt = prefixKeymap.lookup(nextKey);
        if (entryOpt.isPresent()) {
            handleEntry(entryOpt.get(), nextKey);
        }
    }
}
