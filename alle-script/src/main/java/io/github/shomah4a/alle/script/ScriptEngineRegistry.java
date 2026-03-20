package io.github.shomah4a.alle.script;

import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;

/**
 * 言語IDでScriptEngineFactoryを管理するレジストリ。
 */
public class ScriptEngineRegistry {

    private final MutableMap<String, ScriptEngineFactory> factories = Maps.mutable.empty();

    /**
     * ファクトリを登録する。同じ言語IDが既に登録されている場合は上書きする。
     */
    public void register(ScriptEngineFactory factory) {
        factories.put(factory.languageId(), factory);
    }

    /**
     * 指定した言語IDのエンジンを新規生成する。
     */
    public Optional<ScriptEngine> create(String languageId) {
        ScriptEngineFactory factory = factories.get(languageId);
        if (factory == null) {
            return Optional.empty();
        }
        return Optional.of(factory.create());
    }

    /**
     * 登録されている言語IDの一覧を返す。
     */
    public ListIterable<String> availableLanguages() {
        return factories.keysView().toSortedList();
    }
}
