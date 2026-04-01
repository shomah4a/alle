package io.github.shomah4a.alle.core.input;

import java.util.concurrent.CompletableFuture;

/**
 * ユーザーから文字列入力を受け付ける汎用インターフェース。
 * TUIではミニバッファ、GUIではダイアログ等で実装する。
 * 全てのプロンプト呼び出しにはInputHistoryが必須であり、
 * 確定入力は自動的に履歴に追加される。
 */
public interface InputPrompter {

    /**
     * プロンプトを表示してユーザーから文字列入力を受け付ける。
     * CompletableFutureは即座に返り、入力確定またはキャンセル時に完了する。
     *
     * @param message プロンプトメッセージ
     * @param history 入力履歴
     * @return 入力結果
     */
    CompletableFuture<PromptResult> prompt(String message, InputHistory history);

    /**
     * 補完機能付きでプロンプトを表示する。
     * Tabキーで補完候補に基づく補完が行われる。
     *
     * @param message   プロンプトメッセージ
     * @param history   入力履歴
     * @param completer 補完候補プロバイダ
     * @return 入力結果
     */
    default CompletableFuture<PromptResult> prompt(String message, InputHistory history, Completer completer) {
        return prompt(message, history);
    }

    /**
     * 初期値・補完・履歴機能付きでプロンプトを表示する。
     * ユーザー入力エリアにinitialValueが事前入力された状態で開始される。
     * M-p/M-n等で入力履歴をナビゲートでき、確定入力がhistoryに追加される。
     *
     * @param message      プロンプトメッセージ
     * @param initialValue 入力エリアの初期値
     * @param history      入力履歴
     * @param completer    補完候補プロバイダ
     * @return 入力結果
     */
    default CompletableFuture<PromptResult> prompt(
            String message, String initialValue, InputHistory history, Completer completer) {
        return prompt(message, history, completer);
    }

    /**
     * 初期値・補完・履歴・テキスト変更通知機能付きでプロンプトを表示する。
     * ユーザーが文字入力・削除を行うたびにupdateListenerが呼ばれる。
     *
     * @param message        プロンプトメッセージ
     * @param initialValue   入力エリアの初期値
     * @param history        入力履歴
     * @param completer      補完候補プロバイダ
     * @param updateListener テキスト変更時のコールバック
     * @return 入力結果
     */
    default CompletableFuture<PromptResult> prompt(
            String message,
            String initialValue,
            InputHistory history,
            Completer completer,
            InputUpdateListener updateListener) {
        return prompt(message, initialValue, history, completer);
    }
}
