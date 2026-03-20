package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JAR内のリソースをzipimportでPythonモジュールとして読み込めるかの検証。
 */
class ZipImportTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder("python").allowAllAccess(true).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void JAR内のalleパッケージをzipimportで読み込める() {
        // リソースのURLからJARパスとサブディレクトリを抽出
        URL url = getClass().getResource("/io/github/shomah4a/alle/script/alle/__init__.py");
        String urlStr = url.toString();

        // file: プロトコルの場合（テスト時はclasses/ディレクトリから読まれる）
        // jar: プロトコルの場合（fat JAR から読まれる）
        String modulePath;
        if (urlStr.startsWith("jar:file:")) {
            // jar:file:/path/to/app.jar!/io/github/.../alle/__init__.py
            String jarPath = urlStr.substring("jar:file:".length(), urlStr.indexOf("!"));
            modulePath = jarPath + "/io/github/shomah4a/alle/script";
        } else if (urlStr.startsWith("file:")) {
            // file:/path/to/classes/io/github/.../alle/__init__.py
            String filePath = urlStr.substring("file:".length());
            // classes/io/github/shomah4a/alle/script/alle/__init__.py
            // → classes/io/github/shomah4a/alle/script を取得
            modulePath = filePath.replace("/alle/__init__.py", "");
        } else {
            throw new AssertionError("Unexpected URL format: " + urlStr);
        }

        context.getBindings("python").putMember("_module_path", modulePath);
        context.eval("python", """
                import sys
                sys.path.insert(0, _module_path)
                import alle
                """);
        // alleモジュールが読み込めることを確認
        var result = context.eval("python", "alle.__name__");
        assertEquals("alle", result.asString());
    }
}
