package io.github.shomah4a.alle.script;

/**
 * スクリプト評価の結果を表す。
 */
public sealed interface ScriptResult {

    /**
     * 評価成功。
     *
     * @param value 評価結果の文字列表現（結果がない場合は空文字列）
     */
    record Success(String value) implements ScriptResult {}

    /**
     * 評価失敗。
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外（存在する場合）
     */
    record Failure(String message, Throwable cause) implements ScriptResult {}
}
