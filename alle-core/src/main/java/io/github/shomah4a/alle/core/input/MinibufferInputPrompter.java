package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.SelfInsertCommand;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
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

    private static final String COMPLETIONS_BUFFER_NAME = "*Completions*";

    private final Frame frame;
    private @Nullable CompletableFuture<PromptResult> activeFuture;
    private @Nullable Window completionsWindow;
    private @Nullable CompletionsModel completionsModel;

    public MinibufferInputPrompter(Frame frame) {
        this.frame = frame;
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
        var previousActiveWindow = frame.getActiveWindow();
        var minibufferWindow = frame.getMinibufferWindow();
        var minibuffer = minibufferWindow.getBuffer();
        int promptLength = (int) message.codePoints().count();

        // ミニバッファをクリアしてプロンプト文字列と初期値を挿入
        int currentLength = minibuffer.length();
        if (currentLength > 0) {
            minibuffer.deleteText(0, currentLength);
        }
        minibuffer.insertText(0, message + initialValue);
        int initialValueLength = (int) initialValue.codePoints().count();
        minibufferWindow.setPoint(promptLength + initialValueLength);

        // プロンプト文字列をread-onlyに設定
        if (promptLength > 0) {
            minibuffer.putReadOnly(0, promptLength);
        }

        // ミニバッファ用キーマップを作成
        var keymap = createMinibufferKeymap(future, previousActiveWindow, promptLength, history, completer);
        minibuffer.setLocalKeymap(keymap);

        // ミニバッファを有効化
        frame.activateMinibuffer();

        return future;
    }

    private Keymap createMinibufferKeymap(
            CompletableFuture<PromptResult> future,
            Window previousActiveWindow,
            int promptLength,
            InputHistory history,
            @Nullable Completer completer) {
        var keymap = new Keymap("minibuffer");

        // 通常文字入力（Completer提供時はCompletions更新付き）
        if (completer != null) {
            keymap.setDefaultCommand(new MinibufferSelfInsertCommand(completer, promptLength));
        } else {
            keymap.setDefaultCommand(new SelfInsertCommand());
        }

        // RET: 入力確定
        keymap.bind(
                KeyStroke.of('\n'),
                new MinibufferConfirmCommand(future, previousActiveWindow, promptLength, history, completer));

        // C-g: キャンセル
        keymap.bind(KeyStroke.ctrl('g'), new MinibufferCancelCommand(future, previousActiveWindow));

        // Tab: 補完（Completerが提供されている場合のみ）
        if (completer != null) {
            keymap.bind(
                    KeyStroke.of('\t'), new MinibufferCompleteCommand(completer, promptLength, previousActiveWindow));
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

    private void cleanup(Window previousActiveWindow) {
        // *Completions* ウィンドウを閉じる
        closeCompletionsWindow();

        var minibufferWindow = frame.getMinibufferWindow();
        var minibuffer = minibufferWindow.getBuffer();

        // read-onlyプロパティを解除してからクリア
        minibuffer.removeReadOnly(0, minibuffer.length());
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

    /**
     * *Completions* ウィンドウを閉じてモデルをクリアする。
     */
    private void closeCompletionsWindow() {
        if (completionsWindow != null && frame.getWindowTree().contains(completionsWindow)) {
            frame.deleteWindow(completionsWindow);
        }
        completionsWindow = null;
        completionsModel = null;

        // ナビゲーション用キーバインドを解除
        unbindCompletionNavigation();
    }

    /**
     * ミニバッファキーマップにCompletions候補ナビゲーション用バインドを追加する。
     */
    private void bindCompletionNavigation(int promptLength) {
        var keymapOpt = frame.getMinibufferWindow().getBuffer().getLocalKeymap();
        if (keymapOpt.isEmpty()) {
            return;
        }
        var keymap = keymapOpt.get();
        keymap.bind(KeyStroke.ctrl('n'), new CompletionSelectNextCommand(promptLength));
        keymap.bind(KeyStroke.ctrl('p'), new CompletionSelectPreviousCommand(promptLength));
    }

    /**
     * ミニバッファキーマップからCompletions候補ナビゲーション用バインドを解除する。
     */
    private void unbindCompletionNavigation() {
        var keymapOpt = frame.getMinibufferWindow().getBuffer().getLocalKeymap();
        if (keymapOpt.isEmpty()) {
            return;
        }
        var keymap = keymapOpt.get();
        keymap.unbind(KeyStroke.ctrl('n'));
        keymap.unbind(KeyStroke.ctrl('p'));
    }

    /**
     * ミニバッファの入力部分（プロンプト以降）を取得する。
     */
    private String getUserInput(int promptLength) {
        var minibuffer = frame.getMinibufferWindow().getBuffer();
        String fullText = minibuffer.getText();
        int fullLength = (int) fullText.codePoints().count();
        return fullLength > promptLength ? minibuffer.substring(promptLength, fullLength) : "";
    }

    /**
     * ミニバッファの入力部分（プロンプト以降）を置換する。
     */
    private void replaceUserInput(int promptLength, String newInput) {
        var minibufferWindow = frame.getMinibufferWindow();
        var minibuffer = minibufferWindow.getBuffer();
        String fullText = minibuffer.getText();
        int fullLength = (int) fullText.codePoints().count();
        if (fullLength > promptLength) {
            minibuffer.deleteText(promptLength, fullLength - promptLength);
        }
        minibuffer.insertText(promptLength, newInput);
        int newLength = (int) newInput.codePoints().count();
        minibufferWindow.setPoint(promptLength + newLength);
    }

    private class MinibufferConfirmCommand implements Command {

        private final CompletableFuture<PromptResult> future;
        private final Window previousActiveWindow;
        private final int promptLength;
        private final InputHistory history;
        private final @Nullable Completer completer;

        MinibufferConfirmCommand(
                CompletableFuture<PromptResult> future,
                Window previousActiveWindow,
                int promptLength,
                InputHistory history,
                @Nullable Completer completer) {
            this.future = future;
            this.previousActiveWindow = previousActiveWindow;
            this.promptLength = promptLength;
            this.history = history;
            this.completer = completer;
        }

        @Override
        public String name() {
            return "minibuffer-confirm";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            // Completions表示中に選択候補がpartialなら確定せず補完を継続
            if (completionsModel != null) {
                var selected = completionsModel.getSelectedCandidate();
                if (selected != null && !selected.terminal()) {
                    replaceUserInput(promptLength, selected.value());
                    closeCompletionsWindow();
                    return CompletableFuture.completedFuture(null);
                }
            }
            // Completerが提供されている場合、現在の入力に対する候補を確認
            if (completer != null) {
                String userInput = getUserInput(promptLength);
                var candidates = completer.complete(userInput);
                // 入力が完全一致する候補が1件でpartialなら確定しない
                if (candidates.size() == 1
                        && candidates.get(0).value().equals(userInput)
                        && !candidates.get(0).terminal()) {
                    return CompletableFuture.completedFuture(null);
                }
            }

            String userInput = getUserInput(promptLength);
            history.add(userInput);
            cleanup(previousActiveWindow);
            future.complete(new PromptResult.Confirmed(userInput));
            return CompletableFuture.completedFuture(null);
        }
    }

    private class MinibufferCompleteCommand implements Command {

        private final Completer completer;
        private final int promptLength;
        private final Window previousActiveWindow;

        MinibufferCompleteCommand(Completer completer, int promptLength, Window previousActiveWindow) {
            this.completer = completer;
            this.promptLength = promptLength;
            this.previousActiveWindow = previousActiveWindow;
        }

        @Override
        public String name() {
            return "minibuffer-complete";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            String userInput = getUserInput(promptLength);
            var candidates = completer.complete(userInput);
            var outcome = CompletionResult.resolveDetailed(userInput, candidates);

            switch (outcome) {
                case CompletionOutcome.NoMatch ignored -> {
                    // 候補なし: 何もしない
                }
                case CompletionOutcome.Unique unique -> {
                    replaceUserInput(promptLength, unique.candidate().value());
                    if (unique.candidate().terminal()) {
                        // 確定可能な候補: Completionsを閉じる
                        closeCompletionsWindow();
                    }
                    // partial候補: 入力にセットするだけで確定しない（ディレクトリ等）
                }
                case CompletionOutcome.Partial partial -> {
                    if (!partial.commonPrefix().equals(userInput)) {
                        // 補完が進んだ場合: 入力を更新
                        replaceUserInput(promptLength, partial.commonPrefix());
                    } else if (isReTab(context)) {
                        // 補完が進まず再Tab: *Completions* を表示
                        showCompletions(partial.candidates(), context);
                    }
                }
            }

            return CompletableFuture.completedFuture(null);
        }

        private boolean isReTab(CommandContext context) {
            return context.lastCommand().map("minibuffer-complete"::equals).orElse(false);
        }

        private void showCompletions(
                org.eclipse.collections.api.list.ListIterable<CompletionCandidate> candidates, CommandContext context) {
            completionsModel = new CompletionsModel(candidates);
            var displayText = completionsModel.formatForDisplay();

            if (completionsWindow != null && frame.getWindowTree().contains(completionsWindow)) {
                // 既存の *Completions* ウィンドウを更新
                updateCompletionsDisplay();
            } else {
                // 新規に *Completions* ウィンドウを作成
                var buffer = new BufferFacade(
                        new EditableBuffer(COMPLETIONS_BUFFER_NAME, new GapTextModel(), context.settingsRegistry()));
                buffer.insertText(0, displayText);
                completionsWindow = frame.splitWindowBelow(previousActiveWindow, buffer);

                // ナビゲーション用キーバインドを追加
                bindCompletionNavigation(promptLength);
            }
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
            if (firstNavigation) {
                // 初回ナビゲーション時に現在の入力を元入力として保存
                String currentInput = getUserInput(promptLength);
                navigator.updateOriginalInput(currentInput);
                firstNavigation = false;
            }
            navigator.previous().ifPresent(entry -> replaceUserInput(promptLength, entry));
            return CompletableFuture.completedFuture(null);
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
            navigator.next().ifPresent(entry -> replaceUserInput(promptLength, entry));
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

    private class CompletionSelectNextCommand implements Command {

        private final int promptLength;

        CompletionSelectNextCommand(int promptLength) {
            this.promptLength = promptLength;
        }

        @Override
        public String name() {
            return "completion-select-next";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            if (completionsModel == null) {
                return CompletableFuture.completedFuture(null);
            }
            completionsModel.selectNext();
            updateCompletionsDisplay();
            var selected = completionsModel.getSelectedCandidate();
            if (selected != null) {
                replaceUserInput(promptLength, selected.value());
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    private class CompletionSelectPreviousCommand implements Command {

        private final int promptLength;

        CompletionSelectPreviousCommand(int promptLength) {
            this.promptLength = promptLength;
        }

        @Override
        public String name() {
            return "completion-select-previous";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            if (completionsModel == null) {
                return CompletableFuture.completedFuture(null);
            }
            completionsModel.selectPrevious();
            updateCompletionsDisplay();
            var selected = completionsModel.getSelectedCandidate();
            if (selected != null) {
                replaceUserInput(promptLength, selected.value());
            }
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * 文字挿入後にCompletionsバッファを更新するSelfInsertCommandラッパー。
     * *Completions* 表示中に文字入力が行われると、候補リストを再計算して表示を更新する。
     */
    private class MinibufferSelfInsertCommand implements Command {

        private final SelfInsertCommand delegate = new SelfInsertCommand();
        private final Completer completer;
        private final int promptLength;

        MinibufferSelfInsertCommand(Completer completer, int promptLength) {
            this.completer = completer;
            this.promptLength = promptLength;
        }

        @Override
        public String name() {
            return "self-insert-command";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            return delegate.execute(context).thenRun(() -> {
                if (completionsWindow != null && frame.getWindowTree().contains(completionsWindow)) {
                    // *Completions* 表示中: 候補を再計算して更新
                    String userInput = getUserInput(promptLength);
                    var candidates = completer.complete(userInput);
                    if (candidates.isEmpty()) {
                        closeCompletionsWindow();
                    } else {
                        completionsModel = new CompletionsModel(candidates);
                        updateCompletionsDisplay();
                    }
                }
            });
        }
    }

    /**
     * *Completions* バッファの表示テキストを現在のモデル状態で更新する。
     */
    private void updateCompletionsDisplay() {
        if (completionsWindow == null || completionsModel == null) {
            return;
        }
        if (!frame.getWindowTree().contains(completionsWindow)) {
            return;
        }
        var buffer = completionsWindow.getBuffer();
        int length = buffer.length();
        if (length > 0) {
            buffer.deleteText(0, length);
        }
        buffer.insertText(0, completionsModel.formatForDisplay());
        completionsWindow.setPoint(0);
    }
}
