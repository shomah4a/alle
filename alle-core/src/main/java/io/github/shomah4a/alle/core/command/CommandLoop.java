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
import io.github.shomah4a.alle.core.window.FrameActor;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

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
    private final FrameActor frameActor;
    private final BufferManager bufferManager;
    private final InputPrompter inputPrompter;
    private final KillRing killRing;
    private final MessageBuffer messageBuffer;
    private final MessageBuffer warningBuffer;
    private Optional<String> lastCommand = Optional.empty();
    private @Nullable PendingPrefix pendingPrefix;

    /**
     * プレフィックスキー入力待ち状態を表す。
     */
    private record PendingPrefix(Keymap keymap, String displayText) {}

    public CommandLoop(
            InputSource inputSource,
            KeyResolver keyResolver,
            FrameActor frameActor,
            BufferManager bufferManager,
            InputPrompter inputPrompter) {
        this(
                inputSource,
                keyResolver,
                frameActor,
                bufferManager,
                inputPrompter,
                new KillRing(),
                new MessageBuffer("*Messages*", 1000),
                new MessageBuffer("*Warnings*", 1000));
    }

    public CommandLoop(
            InputSource inputSource,
            KeyResolver keyResolver,
            FrameActor frameActor,
            BufferManager bufferManager,
            InputPrompter inputPrompter,
            KillRing killRing,
            MessageBuffer messageBuffer,
            MessageBuffer warningBuffer) {
        this.inputSource = inputSource;
        this.keyResolver = keyResolver;
        this.frameActor = frameActor;
        this.bufferManager = bufferManager;
        this.inputPrompter = inputPrompter;
        this.killRing = killRing;
        this.messageBuffer = messageBuffer;
        this.warningBuffer = warningBuffer;
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
            var unused = processKey(keyOpt.get());
        }
    }

    /**
     * 1つのキーストロークを処理し、コマンド実行のCompletableFutureを返す。
     * コマンドが実行された場合はそのfutureを、それ以外は完了済みfutureを返す。
     * プレフィックスキーの場合は内部状態として保持し、次の呼び出しで解決する。
     */
    public CompletableFuture<Void> processKey(KeyStroke keyStroke) {
        messageBuffer.clearShowingMessage();

        if (pendingPrefix != null) {
            return processPrefixKey(keyStroke);
        } else {
            return processNormalKey(keyStroke);
        }
    }

    private static final CompletableFuture<Void> COMPLETED = CompletableFuture.completedFuture(null);

    private CompletableFuture<Void> processNormalKey(KeyStroke keyStroke) {
        var entryOpt = resolveKey(keyStroke);
        if (entryOpt.isPresent()) {
            return handleEntry(entryOpt.get(), keyStroke, "");
        }
        return COMPLETED;
    }

    private CompletableFuture<Void> processPrefixKey(KeyStroke keyStroke) {
        var prefix = pendingPrefix;
        pendingPrefix = null;
        if (prefix == null) {
            return COMPLETED;
        }

        var entryOpt = prefix.keymap().lookup(keyStroke);
        if (entryOpt.isPresent()) {
            return handleEntry(entryOpt.get(), keyStroke, prefix.displayText());
        }
        return COMPLETED;
    }

    private Optional<KeymapEntry> resolveKey(KeyStroke keyStroke) {
        return frameActor.resolveKey(keyStroke, keyResolver).join();
    }

    private CompletableFuture<Void> handleEntry(KeymapEntry entry, KeyStroke keyStroke, String prefixDisplay) {
        switch (entry) {
            case KeymapEntry.CommandBinding(var command) -> {
                var thisCommand = Optional.of(command.name());
                var context = new CommandContext(
                        frameActor,
                        bufferManager,
                        frameActor.getActiveWindowActor(),
                        inputPrompter,
                        Optional.of(keyStroke),
                        thisCommand,
                        lastCommand,
                        killRing,
                        messageBuffer,
                        warningBuffer);
                return command.execute(context)
                        .thenRun(() -> lastCommand = thisCommand)
                        .exceptionally(ex -> {
                            var cause = ex.getCause() != null ? ex.getCause() : ex;
                            if (cause instanceof ReadOnlyBufferException) {
                                messageBuffer.message("Text is read-only");
                            } else {
                                var message = "コマンド実行中にエラーが発生: " + command.name();
                                logger.log(Level.WARNING, message, ex);
                                context.handleError(message, ex);
                            }
                            return null;
                        });
            }
            case KeymapEntry.PrefixBinding(var prefixKeymap) -> {
                var displayText = prefixDisplay + keyStroke.displayString() + " ";
                pendingPrefix = new PendingPrefix(prefixKeymap, displayText);
                messageBuffer.message(displayText);
                return COMPLETED;
            }
        }
    }
}
