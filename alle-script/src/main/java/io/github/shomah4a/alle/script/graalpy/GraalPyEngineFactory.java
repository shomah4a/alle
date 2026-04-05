package io.github.shomah4a.alle.script.graalpy;

import io.github.shomah4a.alle.script.EditorFacade;
import io.github.shomah4a.alle.script.ScriptEngine;
import io.github.shomah4a.alle.script.ScriptEngineFactory;
import io.github.shomah4a.alle.script.ScriptResult;
import java.io.OutputStream;
import java.nio.file.Path;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GraalPyエンジンのファクトリ。
 * Engineインスタンスを共有することで、複数Context間でコンパイルキャッシュを共有する。
 * テスト等でのリソースリーク防止のためAutoCloseableを実装する。
 */
public class GraalPyEngineFactory implements ScriptEngineFactory, AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GraalPyEngineFactory.class);
    private static final String LANGUAGE_ID = "python";

    private final Engine engine;
    private final EditorFacade editorFacade;
    private final Path alleDotD;
    private final OutputStream stdout;
    private final OutputStream stderr;

    public GraalPyEngineFactory(
            EditorFacade editorFacade,
            Path alleDotD,
            OutputStream stdout,
            OutputStream stderr,
            OutputStream logOutput) {
        this.engine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .logHandler(logOutput)
                .build();
        this.editorFacade = editorFacade;
        this.alleDotD = alleDotD;
        this.stdout = stdout;
        this.stderr = stderr;
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
                .out(stdout)
                .err(stderr)
                .build();
        var pyEngine = new GraalPyEngine(context);
        pyEngine.initAlleModule(editorFacade);
        addUserModulePath(pyEngine);
        return pyEngine;
    }

    private void addUserModulePath(GraalPyEngine pyEngine) {
        Path modulesDir = alleDotD.resolve("modules");
        pyEngine.bind("_user_modules_path", modulesDir.toString());
        var result = pyEngine.eval("import sys; sys.path.append(_user_modules_path); del _user_modules_path");
        if (result instanceof ScriptResult.Failure failure) {
            logger.warn("Failed to add user modules path: {}", failure.message());
        }
    }

    @Override
    public void close() {
        engine.close();
    }
}
