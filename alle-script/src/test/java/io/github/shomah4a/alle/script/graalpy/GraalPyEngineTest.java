package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.shomah4a.alle.script.ScriptResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.graalvm.polyglot.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GraalPyEngineTest {

    private GraalPyEngine engine;

    @BeforeEach
    void setUp() {
        var context = Context.newBuilder("python").allowAllAccess(true).build();
        engine = new GraalPyEngine(context);
    }

    @AfterEach
    void tearDown() {
        engine.close();
    }

    @Test
    void 言語IDはpythonを返す() {
        assertEquals("python", engine.languageId());
    }

    @Test
    void 算術式を評価して結果を返す() {
        ScriptResult result = engine.eval("1 + 2");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("3", ((ScriptResult.Success) result).value());
    }

    @Test
    void 文字列式を評価して結果を返す() {
        ScriptResult result = engine.eval("'hello' + ' ' + 'world'");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("hello world", ((ScriptResult.Success) result).value());
    }

    @Test
    void 構文エラーでFailureを返す() {
        ScriptResult result = engine.eval("def");
        assertInstanceOf(ScriptResult.Failure.class, result);
    }

    @Test
    void バインドしたオブジェクトにスクリプトからアクセスできる() {
        engine.bind("x", 42);
        ScriptResult result = engine.eval("x * 2");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("84", ((ScriptResult.Success) result).value());
    }

    @Test
    void バインドしたJavaオブジェクトのメソッドを呼べる() {
        var sb = new StringBuilder("hello");
        engine.bind("sb", sb);
        engine.eval("sb.append(' world')");
        assertEquals("hello world", sb.toString());
    }

    @Test
    void 文を実行した場合はSuccessで空文字列を返す() {
        ScriptResult result = engine.eval("x = 10");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("", ((ScriptResult.Success) result).value());
    }

    @Test
    void initファイルが存在しない場合はSuccessで空文字列を返す(@TempDir Path tempDir) {
        ScriptResult result = engine.loadUserInit(tempDir);
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("", ((ScriptResult.Success) result).value());
    }

    @Test
    void initファイルが存在する場合はevalされる(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("init.py"), "x = 42");
        ScriptResult result = engine.loadUserInit(tempDir);
        assertInstanceOf(ScriptResult.Success.class, result);
        // init.py で定義した変数にアクセスできる
        ScriptResult varResult = engine.eval("x");
        assertInstanceOf(ScriptResult.Success.class, varResult);
        assertEquals("42", ((ScriptResult.Success) varResult).value());
    }

    @Test
    void initファイルに構文エラーがある場合はFailureを返す(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("init.py"), "def");
        ScriptResult result = engine.loadUserInit(tempDir);
        assertInstanceOf(ScriptResult.Failure.class, result);
    }
}
