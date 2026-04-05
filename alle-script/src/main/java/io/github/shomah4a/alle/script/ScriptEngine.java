package io.github.shomah4a.alle.script;

import java.nio.file.Path;

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

    /**
     * ユーザー設定ディレクトリから初期化スクリプトを読み込む。
     * 初期化ファイル名は言語エンジンごとに決定する（例: Python なら init.py）。
     * 初期化ファイルが存在しない場合は成功として空文字列を返す。
     */
    ScriptResult loadUserInit(Path userConfigDir);

    @Override
    void close();
}
