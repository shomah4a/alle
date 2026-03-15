package io.github.shomah4a.alle.core.input;

import java.util.concurrent.CompletableFuture;

/**
 * ユーザーから文字列入力を受け付ける汎用インターフェース。
 * TUIではミニバッファ、GUIではダイアログ等で実装する。
 */
public interface InputPrompter {

    /**
     * プロンプトを表示してユーザーから文字列入力を受け付ける。
     * CompletableFutureは即座に返り、入力確定またはキャンセル時に完了する。
     *
     * @param message プロンプトメッセージ
     * @return 入力結果
     */
    CompletableFuture<PromptResult> prompt(String message);

    /**
     * 補完機能付きでプロンプトを表示する。
     * Tabキーで補完候補に基づく補完が行われる。
     *
     * @param message   プロンプトメッセージ
     * @param completer 補完候補プロバイダ
     * @return 入力結果
     */
    default CompletableFuture<PromptResult> prompt(String message, Completer completer) {
        return prompt(message);
    }

    /**
     * 初期値と補完機能付きでプロンプトを表示する。
     * ユーザー入力エリアにinitialValueが事前入力された状態で開始される。
     *
     * @param message      プロンプトメッセージ
     * @param initialValue 入力エリアの初期値
     * @param completer    補完候補プロバイダ
     * @return 入力結果
     */
    default CompletableFuture<PromptResult> prompt(String message, String initialValue, Completer completer) {
        return prompt(message, completer);
    }
}
