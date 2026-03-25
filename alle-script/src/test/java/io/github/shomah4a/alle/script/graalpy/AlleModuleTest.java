package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.TextMode;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.script.EditorFacade;
import io.github.shomah4a.alle.script.MessageBufferOutputStream;
import io.github.shomah4a.alle.script.ScriptResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * alleモジュール経由でエディタ操作が動作することの結合テスト。
 */
class AlleModuleTest {

    private GraalPyEngineFactory factory;
    private GraalPyEngine engine;
    private EditableBuffer buffer;
    private BufferManager bufferManager;
    private MessageBuffer messageBuffer;

    @BeforeEach
    void setUp() {
        buffer = new EditableBuffer("test.py", new GapTextModel());
        var bufferFacade = new BufferFacade(buffer);
        var window = new Window(bufferFacade);
        var minibuffer = new Window(new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel())));
        var frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(bufferFacade);
        messageBuffer = new MessageBuffer("*Messages*", 100);

        var facade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new));
        var stdoutStream = new MessageBufferOutputStream(bufferManager, "*Python Output*", 1000);
        var stderrStream = new MessageBufferOutputStream(bufferManager, "*Python Error*", 1000);
        var logStream = new MessageBufferOutputStream(bufferManager, "*Python Log*", 1000);
        factory = new GraalPyEngineFactory(facade, stdoutStream, stderrStream, logStream);
        engine = (GraalPyEngine) factory.create();
    }

    @AfterEach
    void tearDown() {
        engine.close();
        factory.close();
    }

    @Test
    void alleモジュールのimportでactive_windowを取得できる() {
        engine.eval("import alle");
        ScriptResult result = engine.eval("alle.active_window().point()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("0", ((ScriptResult.Success) result).value());
    }

    @Test
    void alleモジュール経由でテキストを挿入できる() {
        engine.eval("import alle");
        engine.eval("alle.active_window().insert('hello')");
        assertEquals("hello", buffer.getText());
    }

    @Test
    void alleモジュール経由でバッファ名を取得できる() {
        engine.eval("import alle");
        ScriptResult result = engine.eval("alle.current_buffer().name()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("test.py", ((ScriptResult.Success) result).value());
    }

    @Test
    void alleモジュール経由でメッセージを表示できる() {
        engine.eval("import alle");
        engine.eval("alle.message('hello from script')");
        assertEquals("hello from script", messageBuffer.lineText(0));
    }

    @Test
    void 同期APIでテキスト挿入とカーソル位置取得ができる() {
        engine.eval("import alle");
        engine.eval("""
                win = alle.active_window()
                win.insert('sync hello')
                pos = win.point()
                """);
        ScriptResult result = engine.eval("pos");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("sync hello", buffer.getText());
    }

    @Test
    void 同期APIでカーソル移動ができる() {
        buffer.insertText(0, "hello world");
        engine.eval("import alle");
        engine.eval("""
                win = alle.active_window()
                win.goto_char(5)
                """);
        ScriptResult result = engine.eval("win.point()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("5", ((ScriptResult.Success) result).value());
    }

    @Test
    void printの出力がstdoutバッファに記録される() {
        engine.eval("print('hello from python')");
        var stdoutBuffer = bufferManager.findByName("*Python Output*").orElseThrow();
        assertEquals("hello from python", stdoutBuffer.lineText(0));
    }

    @Test
    void 複数回のprintが個別に記録される() {
        engine.eval("print('line1')");
        engine.eval("print('line2')");
        var stdoutBuffer = bufferManager.findByName("*Python Output*").orElseThrow();
        assertEquals("line1", stdoutBuffer.lineText(0));
        assertEquals("line2", stdoutBuffer.lineText(1));
    }

    @Test
    void CommandBaseでコマンドを定義して登録できる() {
        engine.eval("import alle");
        engine.eval("from alle.command import CommandBase");
        engine.eval("""
                class TestCmd(CommandBase):
                    def name(self):
                        return "test-cmd"
                    def run(self):
                        alle.message("test-cmd executed")
                """);
        ScriptResult result = engine.eval("alle.register_command(TestCmd())");
        if (result instanceof ScriptResult.Failure f) {
            System.err.println("register_command failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, result);
    }

    @Test
    void MajorModeBaseでメジャーモードを定義して登録できる() {
        engine.eval("import alle");
        engine.eval("from alle.mode import MajorModeBase");
        engine.eval("""
                class TestMode(MajorModeBase):
                    def name(self):
                        return "TestMode"
                """);
        ScriptResult result = engine.eval("alle.register_major_mode(TestMode)");
        if (result instanceof ScriptResult.Failure f) {
            System.err.println("register_major_mode failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, result);
    }

    @Test
    void MajorModeBaseで拡張子マッピング付きでメジャーモードを登録できる() {
        engine.eval("import alle");
        engine.eval("from alle.mode import MajorModeBase");
        engine.eval("""
                class PyMode(MajorModeBase):
                    def name(self):
                        return "Python"
                """);
        ScriptResult result = engine.eval("alle.register_major_mode(PyMode, extensions=['py', 'pyw'])");
        if (result instanceof ScriptResult.Failure f) {
            System.err.println("register_major_mode with extensions failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, result);
    }

    @Test
    void commandデコレータで関数をCommandBaseインスタンスに変換できる() {
        engine.eval("import alle");
        engine.eval("from alle import command");
        engine.eval("""
                @command.command("decorated-cmd")
                def decorated_cmd():
                    alle.message("decorated")
                """);
        ScriptResult result = engine.eval("alle.register_command(decorated_cmd)");
        if (result instanceof ScriptResult.Failure f) {
            System.err.println("register_command with decorator failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, result);
    }

    @Test
    void commandデコレータで生成したコマンドの名前が正しい() {
        engine.eval("from alle import command");
        engine.eval("""
                @command.command("test-name")
                def test_fn():
                    pass
                """);
        ScriptResult result = engine.eval("test_fn.name()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("test-name", ((ScriptResult.Success) result).value());
    }

    @Test
    void 初期化時にPythonモードが自動登録される() {
        engine.eval("import alle");
        ScriptResult result = engine.eval("""
                from alle.modes.python import PythonMode
                mode = PythonMode()
                mode.name()
                """);
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("Python", ((ScriptResult.Success) result).value());
    }

    @Test
    void Pythonモードのスタイラーでコメントをハイライトできる() {
        engine.eval("import alle");
        engine.eval("""
                from alle.modes.python import PythonMode
                mode = PythonMode()
                styler = mode.styler()
                spans = styler.styleLine("x = 1  # comment")
                """);
        ScriptResult result = engine.eval("spans.size()");
        assertInstanceOf(ScriptResult.Success.class, result);
        // コメント部分がスタイリングされている
        int spanCount = Integer.parseInt(((ScriptResult.Success) result).value());
        assertTrue(spanCount > 0, "コメントに対するスパンが生成されるべき");
    }

    @Test
    void Pythonモードのスタイラーでキーワードをハイライトできる() {
        engine.eval("import alle");
        engine.eval("""
                from alle.modes.python import PythonMode
                mode = PythonMode()
                styler = mode.styler()
                spans = styler.styleLine("def hello():")
                """);
        ScriptResult result = engine.eval("spans.size()");
        assertInstanceOf(ScriptResult.Success.class, result);
        int spanCount = Integer.parseInt(((ScriptResult.Success) result).value());
        assertTrue(spanCount > 0, "キーワードに対するスパンが生成されるべき");
    }

    @Test
    void Pythonモードのスタイラーで三重引用符文字列をハイライトできる() {
        engine.eval("import alle");
        ScriptResult evalResult = engine.eval("""
                from alle.modes.python import PythonMode
                mode = PythonMode()
                styler = mode.styler()
                result = styler.styleLineWithState('x = \"\"\"hello', styler.initialState())
                spans = result.spans()
                next_state = result.nextState()
                in_region = next_state.isInRegion()
                """);
        if (evalResult instanceof ScriptResult.Failure f) {
            System.err.println("triple quote test failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, evalResult);
        ScriptResult spanResult = engine.eval("spans.size()");
        assertInstanceOf(ScriptResult.Success.class, spanResult);
        int spanCount = Integer.parseInt(((ScriptResult.Success) spanResult).value());
        assertTrue(spanCount > 0, "三重引用符文字列に対するスパンが生成されるべき");
        // リージョンが継続中であること
        ScriptResult stateResult = engine.eval("str(in_region)");
        assertInstanceOf(ScriptResult.Success.class, stateResult);
        assertEquals("True", ((ScriptResult.Success) stateResult).value());
    }

    @Test
    void MinorModeBaseでマイナーモードを定義して登録できる() {
        engine.eval("import alle");
        engine.eval("from alle.mode import MinorModeBase");
        engine.eval("""
                class TestMinor(MinorModeBase):
                    def name(self):
                        return "TestMinor"
                """);
        ScriptResult result = engine.eval("alle.register_minor_mode(TestMinor)");
        if (result instanceof ScriptResult.Failure f) {
            System.err.println("register_minor_mode failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, result);
    }
}
