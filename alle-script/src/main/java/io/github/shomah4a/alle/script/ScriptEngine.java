package io.github.shomah4a.alle.script;

/**
 * スクリプト言語エンジンの抽象インターフェース。
 * 言語ごとの実装を差し替え可能にする。
 */
public interface ScriptEngine extends AutoCloseable {

    /**
     * この言語エンジンの識別子を返す（例: "python", "js"）。
     */
    String languageId();

    /**
     * スクリプトコードを評価する。
     */
    ScriptResult eval(String code);

    /**
     * スクリプトコンテキストに名前付きオブジェクトをバインドする。
     * バインドされたオブジェクトはスクリプトからグローバル変数としてアクセスできる。
     */
    void bind(String name, Object value);

    @Override
    void close();
}
