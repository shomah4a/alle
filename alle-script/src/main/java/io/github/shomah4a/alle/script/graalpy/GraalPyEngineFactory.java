package io.github.shomah4a.alle.script.graalpy;

import io.github.shomah4a.alle.script.ScriptEngine;
import io.github.shomah4a.alle.script.ScriptEngineFactory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

/**
 * GraalPyエンジンのファクトリ。
 * Engineインスタンスを共有することで、複数Context間でコンパイルキャッシュを共有する。
 */
public class GraalPyEngineFactory implements ScriptEngineFactory {

    private static final String LANGUAGE_ID = "python";

    private final Engine engine;

    public GraalPyEngineFactory() {
        this.engine = Engine.create();
    }

    @Override
    public String languageId() {
        return LANGUAGE_ID;
    }

    @Override
    public ScriptEngine create() {
        Context context = Context.newBuilder(LANGUAGE_ID)
                .engine(engine)
                .allowAllAccess(true)
                .build();
        return new GraalPyEngine(context);
    }
}
