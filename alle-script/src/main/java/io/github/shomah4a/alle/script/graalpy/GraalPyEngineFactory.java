package io.github.shomah4a.alle.script.graalpy;

import io.github.shomah4a.alle.script.EditorFacade;
import io.github.shomah4a.alle.script.ScriptEngine;
import io.github.shomah4a.alle.script.ScriptEngineFactory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;

/**
 * GraalPyエンジンのファクトリ。
 * Engineインスタンスを共有することで、複数Context間でコンパイルキャッシュを共有する。
 * テスト等でのリソースリーク防止のためAutoCloseableを実装する。
 */
public class GraalPyEngineFactory implements ScriptEngineFactory, AutoCloseable {

    private static final String LANGUAGE_ID = "python";

    private final Engine engine;
    private final EditorFacade editorFacade;

    public GraalPyEngineFactory(EditorFacade editorFacade) {
        this.engine = Engine.create();
        this.editorFacade = editorFacade;
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
        var pyEngine = new GraalPyEngine(context);
        pyEngine.initAlleModule(editorFacade);
        return pyEngine;
    }

    @Override
    public void close() {
        engine.close();
    }
}
