package io.github.shomah4a.alle.script.graalpy;

import io.github.shomah4a.alle.script.ScriptEngine;
import io.github.shomah4a.alle.script.ScriptResult;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GraalVM Polyglot APIを使ったPythonスクリプトエンジン。
 */
public class GraalPyEngine implements ScriptEngine {

    private static final Logger logger = LoggerFactory.getLogger(GraalPyEngine.class);
    private static final String LANGUAGE_ID = "python";

    private final Context context;

    GraalPyEngine(Context context) {
        this.context = context;
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
}
