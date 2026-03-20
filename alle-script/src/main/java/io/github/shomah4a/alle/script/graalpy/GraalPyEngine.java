package io.github.shomah4a.alle.script.graalpy;

import io.github.shomah4a.alle.script.EditorFacade;
import io.github.shomah4a.alle.script.ScriptEngine;
import io.github.shomah4a.alle.script.ScriptResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GraalVM Polyglot APIを使ったPythonスクリプトエンジン。
 */
public class GraalPyEngine implements ScriptEngine {

    private static final Logger logger = LoggerFactory.getLogger(GraalPyEngine.class);
    private static final String LANGUAGE_ID = "python";
    private static final String ALLE_MODULE_RESOURCE = "/io/github/shomah4a/alle/script/alle.py";

    private final Context context;

    GraalPyEngine(Context context) {
        this.context = context;
    }

    /**
     * alleモジュールをロードし、EditorFacadeを注入する。
     * エンジン生成後、スクリプト実行前に呼ぶこと。
     */
    public void initAlleModule(EditorFacade editorFacade) {
        String moduleSource = loadResource(ALLE_MODULE_RESOURCE);
        // alleモジュールをsys.modulesに登録し、import alleで使えるようにする
        bind("_alle_module_source", moduleSource);
        bind("_editor_facade", editorFacade);
        context.eval(LANGUAGE_ID, """
                import types, sys
                _alle_mod = types.ModuleType('alle')
                _alle_mod.__dict__['__name__'] = 'alle'
                exec(_alle_module_source, _alle_mod.__dict__)
                sys.modules['alle'] = _alle_mod
                _alle_mod._init(_editor_facade)
                del _alle_module_source
                """);
    }

    @Override
    public String languageId() {
        return LANGUAGE_ID;
    }

    @Override
    public ScriptResult eval(String code) {
        try {
            Value result = context.eval(LANGUAGE_ID, code);
            String valueStr;
            if (result.isNull()) {
                valueStr = "";
            } else if (result.isString()) {
                valueStr = result.asString();
            } else if (result.isNumber()) {
                valueStr = result.toString();
            } else if (result.isBoolean()) {
                valueStr = result.toString();
            } else {
                valueStr = "";
            }
            return new ScriptResult.Success(valueStr);
        } catch (PolyglotException e) {
            logger.debug("スクリプト評価エラー", e);
            String message =
                    e.getMessage() != null ? e.getMessage() : e.getClass().getName();
            return new ScriptResult.Failure(message, e);
        }
    }

    @Override
    public void bind(String name, Object value) {
        context.getBindings(LANGUAGE_ID).putMember(name, value);
    }

    @Override
    public void close() {
        context.close();
    }

    private static String loadResource(String path) {
        @Nullable InputStream is = GraalPyEngine.class.getResourceAsStream(path);
        if (is == null) {
            throw new IllegalStateException("リソースが見つかりません: " + path);
        }
        try (is) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("リソースの読み込みに失敗しました: " + path, e);
        }
    }
}
