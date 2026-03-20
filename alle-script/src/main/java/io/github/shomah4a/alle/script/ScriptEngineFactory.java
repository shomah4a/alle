package io.github.shomah4a.alle.script;

/**
 * ScriptEngineのファクトリ。
 * 言語ごとに実装を提供し、ScriptEngineRegistryに登録する。
 */
public interface ScriptEngineFactory {

    /**
     * この言語エンジンの識別子を返す。
     */
    String languageId();

    /**
     * 新しいScriptEngineインスタンスを生成する。
     */
    ScriptEngine create();
}
