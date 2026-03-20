package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.shomah4a.alle.script.ScriptResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GraalPyEngineTest {

    private GraalPyEngine engine;

    @BeforeEach
    void setUp() {
        var factory = new GraalPyEngineFactory();
        engine = (GraalPyEngine) factory.create();
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
}
