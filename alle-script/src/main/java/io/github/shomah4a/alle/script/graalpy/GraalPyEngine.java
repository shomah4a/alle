package io.github.shomah4a.alle.script.graalpy;

import io.github.shomah4a.alle.script.EditorFacade;
import io.github.shomah4a.alle.script.ScriptEngine;
import io.github.shomah4a.alle.script.ScriptResult;
import java.net.URL;
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
    private static final String ALLE_INIT_RESOURCE = "/io/github/shomah4a/alle/script/alle/__init__.py";

    private final Context context;

    GraalPyEngine(Context context) {
        this.context = context;
    }

    /**
     * alleモジュールをロードし、EditorFacadeを注入する。
     * エンジン生成後、スクリプト実行前に呼ぶこと。
     *
     * <p>リソースのURLからモジュール検索パスを解決する。
     * JAR内の場合はzipimport経由、classesディレクトリの場合はファイルパス経由で読み込む。
     */
    public void initAlleModule(EditorFacade editorFacade) {
        String modulePath = resolveModulePath();
        bind("_editor_facade", editorFacade);
        bind("_alle_module_path", modulePath);
        context.eval(LANGUAGE_ID, """
                import sys
                sys.path.insert(0, _alle_module_path)
                del _alle_module_path
                import alle
                alle._init(_editor_facade)
                del _editor_facade
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

    /**
     * alleパッケージの親ディレクトリのパスを解決する。
     * JAR内の場合: /path/to/app.jar/io/github/shomah4a/alle/script
     * classesの場合: /path/to/classes/io/github/shomah4a/alle/script
     */
    private static String resolveModulePath() {
        @Nullable URL url = GraalPyEngine.class.getResource(ALLE_INIT_RESOURCE);
        if (url == null) {
            throw new IllegalStateException("alleモジュールのリソースが見つかりません: " + ALLE_INIT_RESOURCE);
        }
        String urlStr = url.toString();
        // alle/__init__.py の親の親（= alle パッケージの親）を取得
        String suffix = "/alle/__init__.py";
        if (urlStr.startsWith("jar:file:")) {
            // jar:file:/path/to/app.jar!/io/github/.../alle/__init__.py
            String inner = urlStr.substring("jar:file:".length());
            String jarPath = inner.substring(0, inner.indexOf("!"));
            String resourceDir = inner.substring(inner.indexOf("!") + 1);
            return jarPath + resourceDir.replace(suffix, "");
        } else if (urlStr.startsWith("file:")) {
            // file:/path/to/classes/io/github/.../alle/__init__.py
            String filePath = urlStr.substring("file:".length());
            return filePath.replace(suffix, "");
        } else {
            throw new IllegalStateException("未対応のURLスキーム: " + urlStr);
        }
    }
}
