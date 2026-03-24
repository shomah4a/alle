package io.github.shomah4a.alle.tui;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.SelfInsertCommand;
import io.github.shomah4a.alle.core.input.Completer;
import io.github.shomah4a.alle.core.input.CompletionResult;
import io.github.shomah4a.alle.core.input.HistoryNavigator;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.window.FrameActor;
import io.github.shomah4a.alle.core.window.WindowActor;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * ミニバッファウィンドウを使ったInputPrompter実装。
 * プロンプト文字列をミニバッファに表示し、ユーザーの入力を受け付ける。
 * RETで確定、C-gでキャンセル。入力中もメインのCommandLoopは動き続ける。
 */
public class MinibufferInputPrompter implements InputPrompter {

    private static final Logger logger = Logger.getLogger(MinibufferInputPrompter.class.getName());

    private final FrameActor frameActor;
    private @Nullable CompletableFuture<PromptResult> activeFuture;

    public MinibufferInputPrompter(FrameActor frameActor) {
        this.frameActor = frameActor;
    }

    @Override
    public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
        return promptInternal(message, "", history, null);
    }

    @Override
    public CompletableFuture<PromptResult> prompt(String message, InputHistory history, Completer completer) {
        return promptInternal(message, "", history, completer);
    }

    @Override
    public CompletableFuture<PromptResult> prompt(
            String message, String initialValue, InputHistory history, Completer completer) {
        return promptInternal(message, initialValue, history, completer);
    }

    private CompletableFuture<PromptResult> promptInternal(
            String message, String initialValue, InputHistory history, @Nullable Completer completer) {
        if (activeFuture != null && !activeFuture.isDone()) {
            logger.warning("別のプロンプトがアクティブなため後続のプロンプトをキャンセルしました: " + message);
            return CompletableFuture.completedFuture(new PromptResult.Cancelled());
        }
        var future = new CompletableFuture<PromptResult>();
        activeFuture = future;
        var previousActiveWindowActor = frameActor.getActiveWindowActor();
        var minibufferActor = frameActor.getMinibufferWindowActor();
        var bufferActor = minibufferActor.getBufferActor();
        int promptLength = (int) message.codePoints().count();
        int initialValueLength = (int) initialValue.codePoints().count();

        // ミニバッファをクリアしてプロンプト文字列と初期値を挿入
        return bufferActor
                .length()
                .thenCompose(currentLength -> {
                    if (currentLength > 0) {
                        return bufferActor.deleteText(0, currentLength).thenApply(c -> null);
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenCompose(v -> bufferActor.insertText(0, message + initialValue))
                .thenCompose(c -> minibufferActor.setPoint(promptLength + initialValueLength))
                // プロンプト文字列をread-onlyに設定
                .thenCompose(v -> {
                    if (promptLength > 0) {
                        return bufferActor.putReadOnly(0, promptLength);
                    }
                    return CompletableFuture.completedFuture(null);
                })
                // ミニバッファ用キーマップを作成・設定
                .thenCompose(v -> {
                    var keymap =
                            createMinibufferKeymap(future, previousActiveWindowActor, promptLength, history, completer);
                    return bufferActor.setLocalKeymap(keymap);
                })
                // ミニバッファを有効化
                .thenCompose(v -> frameActor.activateMinibuffer())
                // ユーザーの入力完了を待つ
                .thenCompose(v -> future);
    }

    private Keymap createMinibufferKeymap(
            CompletableFuture<PromptResult> future,
            WindowActor previousActiveWindowActor,
            int promptLength,
            InputHistory history,
            @Nullable Completer completer) {
        var keymap = new Keymap("minibuffer");

        // 通常文字入力
        keymap.setDefaultCommand(new SelfInsertCommand());

        // RET: 入力確定
        keymap.bind(
                KeyStroke.of('\n'),
                new MinibufferConfirmCommand(future, previousActiveWindowActor, promptLength, history));

        // C-g: キャンセル
        keymap.bind(KeyStroke.ctrl('g'), new MinibufferCancelCommand(future, previousActiveWindowActor));

        // Tab: 補完（Completerが提供されている場合のみ）
        if (completer != null) {
            keymap.bind(KeyStroke.of('\t'), new MinibufferCompleteCommand(completer, promptLength));
        }

        // ヒストリナビゲーション
        var navigator = new HistoryNavigator(history, "");
        var prevCommand = new MinibufferPreviousHistoryCommand(navigator, promptLength);
        var nextCommand = new MinibufferNextHistoryCommand(navigator, promptLength);
        keymap.bind(KeyStroke.meta('p'), prevCommand);
        keymap.bind(KeyStroke.of(KeyStroke.ARROW_UP), prevCommand);
        keymap.bind(KeyStroke.meta('n'), nextCommand);
        keymap.bind(KeyStroke.of(KeyStroke.ARROW_DOWN), nextCommand);

        return keymap;
    }

    private CompletableFuture<Void> cleanup(WindowActor previousActiveWindowActor) {
        var minibufferActor = frameActor.getMinibufferWindowActor();
        var bufferActor = minibufferActor.getBufferActor();

        // read-onlyプロパティを解除してからクリア
        return bufferActor
                .length()
                .thenCompose(length -> bufferActor.removeReadOnly(0, length).thenApply(v -> length))
                .thenCompose(length -> {
                    if (length > 0) {
                        return bufferActor.deleteText(0, length).thenApply(c -> null);
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenCompose(v -> minibufferActor.setPoint(0))
                // キーマップを解除
                .thenCompose(v -> bufferActor.clearLocalKeymap())
                // ミニバッファを無効化して元のウィンドウに戻す
                .thenCompose(v -> frameActor.deactivateMinibuffer())
                .thenCompose(v -> frameActor.setActiveWindow(previousActiveWindowActor))
                .thenRun(() -> activeFuture = null);
    }

    /**
     * ミニバッファの入力部分（プロンプト以降）を取得する。
     */
    private CompletableFuture<String> getUserInput(int promptLength) {
        var bufferActor = frameActor.getMinibufferWindowActor().getBufferActor();
        return bufferActor.getText().thenCompose(fullText -> {
            int fullLength = (int) fullText.codePoints().count();
            if (fullLength > promptLength) {
                return bufferActor.substring(promptLength, fullLength);
            }
            return CompletableFuture.completedFuture("");
        });
    }

    /**
     * ミニバッファの入力部分（プロンプト以降）を置換する。
     */
    private CompletableFuture<Void> replaceUserInput(int promptLength, String newInput) {
        var minibufferActor = frameActor.getMinibufferWindowActor();
        var bufferActor = minibufferActor.getBufferActor();
        return bufferActor
                .getText()
                .thenCompose(fullText -> {
                    int fullLength = (int) fullText.codePoints().count();
                    if (fullLength > promptLength) {
                        return bufferActor
                                .deleteText(promptLength, fullLength - promptLength)
                                .thenApply(c -> null);
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenCompose(v -> bufferActor.insertText(promptLength, newInput))
                .thenCompose(c -> {
                    int newLength = (int) newInput.codePoints().count();
                    return minibufferActor.setPoint(promptLength + newLength);
                });
    }

    private class MinibufferConfirmCommand implements Command {

        private final CompletableFuture<PromptResult> future;
        private final WindowActor previousActiveWindowActor;
        private final int promptLength;
        private final InputHistory history;

        MinibufferConfirmCommand(
                CompletableFuture<PromptResult> future,
                WindowActor previousActiveWindowActor,
                int promptLength,
                InputHistory history) {
            this.future = future;
            this.previousActiveWindowActor = previousActiveWindowActor;
            this.promptLength = promptLength;
            this.history = history;
        }

        @Override
        public String name() {
            return "minibuffer-confirm";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            return getUserInput(promptLength).thenCompose(userInput -> {
                history.add(userInput);
                return cleanup(previousActiveWindowActor)
                        .thenRun(() -> future.complete(new PromptResult.Confirmed(userInput)));
            });
        }
    }

    private class MinibufferCompleteCommand implements Command {

        private final Completer completer;
        private final int promptLength;

        MinibufferCompleteCommand(Completer completer, int promptLength) {
            this.completer = completer;
            this.promptLength = promptLength;
        }

        @Override
        public String name() {
            return "minibuffer-complete";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            return getUserInput(promptLength).thenCompose(userInput -> {
                var candidates = completer.complete(userInput);
                String completed = CompletionResult.resolve(userInput, candidates);
                if (!completed.equals(userInput)) {
                    return replaceUserInput(promptLength, completed);
                }
                return CompletableFuture.completedFuture(null);
            });
        }
    }

    private class MinibufferPreviousHistoryCommand implements Command {

        private final HistoryNavigator navigator;
        private final int promptLength;
        private boolean firstNavigation = true;

        MinibufferPreviousHistoryCommand(HistoryNavigator navigator, int promptLength) {
            this.navigator = navigator;
            this.promptLength = promptLength;
        }

        @Override
        public String name() {
            return "minibuffer-previous-history";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            CompletableFuture<Void> prepareFuture;
            if (firstNavigation) {
                prepareFuture = getUserInput(promptLength).thenAccept(currentInput -> {
                    navigator.updateOriginalInput(currentInput);
                    firstNavigation = false;
                });
            } else {
                prepareFuture = CompletableFuture.completedFuture(null);
            }
            return prepareFuture.thenCompose(v -> {
                var entryOpt = navigator.previous();
                if (entryOpt.isPresent()) {
                    return replaceUserInput(promptLength, entryOpt.get());
                }
                return CompletableFuture.completedFuture(null);
            });
        }
    }

    private class MinibufferNextHistoryCommand implements Command {

        private final HistoryNavigator navigator;
        private final int promptLength;

        MinibufferNextHistoryCommand(HistoryNavigator navigator, int promptLength) {
            this.navigator = navigator;
            this.promptLength = promptLength;
        }

        @Override
        public String name() {
            return "minibuffer-next-history";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            var entryOpt = navigator.next();
            if (entryOpt.isPresent()) {
                return replaceUserInput(promptLength, entryOpt.get());
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private class MinibufferCancelCommand implements Command {

        private final CompletableFuture<PromptResult> future;
        private final WindowActor previousActiveWindowActor;

        MinibufferCancelCommand(CompletableFuture<PromptResult> future, WindowActor previousActiveWindowActor) {
            this.future = future;
            this.previousActiveWindowActor = previousActiveWindowActor;
        }

        @Override
        public String name() {
            return "minibuffer-cancel";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            return cleanup(previousActiveWindowActor).thenRun(() -> future.complete(new PromptResult.Cancelled()));
        }
    }
}
