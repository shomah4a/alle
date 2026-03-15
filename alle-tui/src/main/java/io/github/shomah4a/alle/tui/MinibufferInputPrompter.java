package io.github.shomah4a.alle.tui;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.SelfInsertCommand;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
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

    private final Frame frame;
    private @Nullable CompletableFuture<PromptResult> activeFuture;

    public MinibufferInputPrompter(Frame frame) {
        this.frame = frame;
    }

    @Override
    public CompletableFuture<PromptResult> prompt(String message) {
        if (activeFuture != null && !activeFuture.isDone()) {
            logger.warning("別のプロンプトがアクティブなため後続のプロンプトをキャンセルしました: " + message);
            return CompletableFuture.completedFuture(new PromptResult.Cancelled());
        }
        var future = new CompletableFuture<PromptResult>();
        activeFuture = future;
        var previousActiveWindow = frame.getActiveWindow();
        var minibufferWindow = frame.getMinibufferWindow();
        var minibuffer = minibufferWindow.getBuffer();
        int promptLength = (int) message.codePoints().count();

        // ミニバッファをクリアしてプロンプト文字列を挿入
        int currentLength = minibuffer.length();
        if (currentLength > 0) {
            minibuffer.deleteText(0, currentLength);
        }
        minibuffer.insertText(0, message);
        minibufferWindow.setPoint(promptLength);

        // ミニバッファ用キーマップを作成
        var keymap = createMinibufferKeymap(future, previousActiveWindow, promptLength);
        minibuffer.setLocalKeymap(keymap);

        // ミニバッファを有効化
        frame.activateMinibuffer();

        return future;
    }

    private Keymap createMinibufferKeymap(
            CompletableFuture<PromptResult> future, Window previousActiveWindow, int promptLength) {
        var keymap = new Keymap("minibuffer");

        // 通常文字入力
        keymap.setDefaultCommand(new SelfInsertCommand());

        // RET: 入力確定
        keymap.bind(KeyStroke.of('\n'), new MinibufferConfirmCommand(future, previousActiveWindow, promptLength));

        // C-g: キャンセル
        keymap.bind(KeyStroke.ctrl('g'), new MinibufferCancelCommand(future, previousActiveWindow));

        return keymap;
    }

    private void cleanup(Window previousActiveWindow) {
        var minibufferWindow = frame.getMinibufferWindow();
        var minibuffer = minibufferWindow.getBuffer();

        // ミニバッファをクリア
        int length = minibuffer.length();
        if (length > 0) {
            minibuffer.deleteText(0, length);
        }
        minibufferWindow.setPoint(0);

        // キーマップを解除
        minibuffer.clearLocalKeymap();

        // ミニバッファを無効化して元のウィンドウに戻す
        frame.deactivateMinibuffer();
        frame.setActiveWindow(previousActiveWindow);

        activeFuture = null;
    }

    private class MinibufferConfirmCommand implements Command {

        private final CompletableFuture<PromptResult> future;
        private final Window previousActiveWindow;
        private final int promptLength;

        MinibufferConfirmCommand(
                CompletableFuture<PromptResult> future, Window previousActiveWindow, int promptLength) {
            this.future = future;
            this.previousActiveWindow = previousActiveWindow;
            this.promptLength = promptLength;
        }

        @Override
        public String name() {
            return "minibuffer-confirm";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            var minibuffer = frame.getMinibufferWindow().getBuffer();
            String fullText = minibuffer.getText();
            int fullLength = (int) fullText.codePoints().count();
            String userInput = fullLength > promptLength ? minibuffer.substring(promptLength, fullLength) : "";

            cleanup(previousActiveWindow);
            future.complete(new PromptResult.Confirmed(userInput));
            return CompletableFuture.completedFuture(null);
        }
    }

    private class MinibufferCancelCommand implements Command {

        private final CompletableFuture<PromptResult> future;
        private final Window previousActiveWindow;

        MinibufferCancelCommand(CompletableFuture<PromptResult> future, Window previousActiveWindow) {
            this.future = future;
            this.previousActiveWindow = previousActiveWindow;
        }

        @Override
        public String name() {
            return "minibuffer-cancel";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            cleanup(previousActiveWindow);
            future.complete(new PromptResult.Cancelled());
            return CompletableFuture.completedFuture(null);
        }
    }
}
