package io.github.shomah4a.alle.core.input;

/**
 * プロンプト入力の結果。
 * ユーザーが入力を確定したか、キャンセルしたかを型で区別する。
 */
public sealed interface PromptResult {

    /**
     * ユーザーが入力を確定した。
     */
    record Confirmed(String value) implements PromptResult {}

    /**
     * ユーザーが入力をキャンセルした。
     */
    record Cancelled() implements PromptResult {}
}
