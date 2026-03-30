package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.TextMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
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
    private TextBuffer buffer;
    private BufferManager bufferManager;
    private MessageBuffer messageBuffer;

    @BeforeEach
    void setUp() {
        buffer = new TextBuffer("test.py", new GapTextModel(), new SettingsRegistry());
        var bufferFacade = new BufferFacade(buffer);
        var window = new Window(bufferFacade);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        var frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(bufferFacade);
        messageBuffer = new MessageBuffer("*Messages*", 100, new SettingsRegistry());

        var facade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new),
                io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry.createWithBuiltins());
        var stdoutStream =
                new MessageBufferOutputStream(bufferManager, "*Python Output*", 1000, new SettingsRegistry());
        var stderrStream = new MessageBufferOutputStream(bufferManager, "*Python Error*", 1000, new SettingsRegistry());
        var logStream = new MessageBufferOutputStream(bufferManager, "*Python Log*", 1000, new SettingsRegistry());
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
                        return "python"
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
        assertEquals("python", ((ScriptResult.Success) result).value());
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
        engine.eval("import java");
        ScriptResult evalResult = engine.eval("""
                from alle.modes.python import PythonMode
                Lists = java.type('org.eclipse.collections.api.factory.Lists')
                mode = PythonMode()
                styler = mode.styler()
                lines = Lists.immutable.of('x = \"\"\"hello', 'world', '\"\"\"')
                doc_result = styler.styleDocument(lines)
                # 各行にスパンがあるか確認
                span_count_line0 = doc_result.get(0).size()
                span_count_line1 = doc_result.get(1).size()
                span_count_line2 = doc_result.get(2).size()
                """);
        if (evalResult instanceof ScriptResult.Failure f) {
            System.err.println("triple quote test failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, evalResult);
        // 各行に文字列スパンが存在すること
        ScriptResult line0Result = engine.eval("str(span_count_line0)");
        assertInstanceOf(ScriptResult.Success.class, line0Result);
        assertTrue(Integer.parseInt(((ScriptResult.Success) line0Result).value()) > 0, "1行目に三重引用符文字列のスパンが生成されるべき");
        ScriptResult line1Result = engine.eval("str(span_count_line1)");
        assertInstanceOf(ScriptResult.Success.class, line1Result);
        assertTrue(Integer.parseInt(((ScriptResult.Success) line1Result).value()) > 0, "2行目（文字列内部）にスパンが生成されるべき");
    }

    @Test
    void Pythonモードがキーマップを持つ() {
        engine.eval("import alle");
        ScriptResult result = engine.eval("""
                from alle.modes.python import PythonMode
                mode = PythonMode()
                km = mode.keymap()
                str(km is not None)
                """);
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("True", ((ScriptResult.Success) result).value());
    }

    @Test
    void newlineAndIndentで前行のインデントが継承される() {
        engine.eval("import alle");
        buffer.insertText(0, "    x = 1");
        // カーソルを行末に移動
        engine.eval("alle.active_window().goto_char(9)");
        // newline-and-indent を実行
        ScriptResult result = engine.eval("""
                from alle.modes.python.commands import PythonIndentState
                state = PythonIndentState(None)
                win = alle.active_window()
                state.newline_and_indent(win, win.buffer())
                """);
        if (result instanceof ScriptResult.Failure f) {
            System.err.println("newline-and-indent failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("    x = 1\n    ", buffer.getText());
    }

    @Test
    void newlineAndIndentでコロン後にインデントが増加する() {
        engine.eval("import alle");
        buffer.insertText(0, "def hello():");
        engine.eval("alle.active_window().goto_char(12)");
        ScriptResult result = engine.eval("""
                from alle.modes.python.commands import PythonIndentState
                state = PythonIndentState(None)
                win = alle.active_window()
                state.newline_and_indent(win, win.buffer())
                """);
        if (result instanceof ScriptResult.Failure f) {
            System.err.println("newline-and-indent colon failed: " + f.message());
            f.cause().printStackTrace(System.err);
        }
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("def hello():\n    ", buffer.getText());
    }

    @Test
    void newlineAndIndentでコメント内のコロンではインデント増加しない() {
        engine.eval("import alle");
        buffer.insertText(0, "x = 1  # Note: something");
        engine.eval("alle.active_window().goto_char(24)");
        ScriptResult result = engine.eval("""
                from alle.modes.python.commands import PythonIndentState
                state = PythonIndentState(None)
                win = alle.active_window()
                state.newline_and_indent(win, win.buffer())
                """);
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("x = 1  # Note: something\n", buffer.getText());
    }

    @Test
    void ElectricPairで開き括弧入力時に閉じ括弧が自動挿入される() {
        engine.eval("import alle");
        engine.eval("""
                from alle.modes.electric_pair.commands import open_paren
                open_paren.run(None)
                """);
        assertEquals("()", buffer.getText());
        // カーソルが括弧の間にある
        ScriptResult result = engine.eval("alle.active_window().point()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("1", ((ScriptResult.Success) result).value());
    }

    @Test
    void ElectricPairで閉じ括弧入力時に次の文字が同じならスキップする() {
        engine.eval("import alle");
        buffer.insertText(0, "()");
        // カーソルを括弧の間に置く
        engine.eval("alle.active_window().goto_char(1)");
        engine.eval("""
                from alle.modes.electric_pair.commands import close_paren
                close_paren.run(None)
                """);
        // テキストは変わらず、カーソルが閉じ括弧の後ろに移動
        assertEquals("()", buffer.getText());
        ScriptResult result = engine.eval("alle.active_window().point()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("2", ((ScriptResult.Success) result).value());
    }

    @Test
    void ElectricPairでダブルクォート入力時にペアが挿入される() {
        engine.eval("import alle");
        engine.eval("""
                from alle.modes.electric_pair.commands import insert_double_quote
                insert_double_quote.run(None)
                """);
        assertEquals("\"\"", buffer.getText());
        ScriptResult result = engine.eval("alle.active_window().point()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("1", ((ScriptResult.Success) result).value());
    }

    @Test
    void ElectricPairでダブルクォート入力時に次の文字が同じならスキップする() {
        engine.eval("import alle");
        buffer.insertText(0, "\"\"");
        engine.eval("alle.active_window().goto_char(1)");
        engine.eval("""
                from alle.modes.electric_pair.commands import insert_double_quote
                insert_double_quote.run(None)
                """);
        assertEquals("\"\"", buffer.getText());
        ScriptResult result = engine.eval("alle.active_window().point()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("2", ((ScriptResult.Success) result).value());
    }

    @Test
    void ElectricPairで角括弧のペアが挿入される() {
        engine.eval("import alle");
        engine.eval("""
                from alle.modes.electric_pair.commands import open_bracket
                open_bracket.run(None)
                """);
        assertEquals("[]", buffer.getText());
    }

    @Test
    void ElectricPairで波括弧のペアが挿入される() {
        engine.eval("import alle");
        engine.eval("""
                from alle.modes.electric_pair.commands import open_brace
                open_brace.run(None)
                """);
        assertEquals("{}", buffer.getText());
    }

    @Test
    void ElectricPairでシングルクォートが単語の後ではペア挿入されない() {
        engine.eval("import alle");
        buffer.insertText(0, "it");
        engine.eval("alle.active_window().goto_char(2)");
        engine.eval("""
                from alle.modes.electric_pair.commands import insert_single_quote
                insert_single_quote.run(None)
                """);
        assertEquals("it'", buffer.getText());
    }

    @Test
    void ElectricPairで閉じ括弧スキップは未対応の開き括弧がある場合のみ動作する() {
        engine.eval("import alle");
        // 未対応の開き括弧がない状態で ) の前にカーソルを置く
        buffer.insertText(0, "x)");
        engine.eval("alle.active_window().goto_char(1)");
        engine.eval("""
                from alle.modes.electric_pair.commands import close_paren
                close_paren.run(None)
                """);
        // 未対応の開き括弧がないので ) が挿入される
        assertEquals("x))", buffer.getText());
    }

    @Test
    void ElectricPairで閉じ括弧スキップは未対応の開き括弧がある場合にスキップする() {
        engine.eval("import alle");
        // 未対応の開き括弧がある状態
        buffer.insertText(0, "(x)");
        engine.eval("alle.active_window().goto_char(2)");
        engine.eval("""
                from alle.modes.electric_pair.commands import close_paren
                close_paren.run(None)
                """);
        // 未対応の開き括弧があるのでスキップ
        assertEquals("(x)", buffer.getText());
        ScriptResult result = engine.eval("alle.active_window().point()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("3", ((ScriptResult.Success) result).value());
    }

    @Test
    void TABでインデントが調整される() {
        engine.eval("import alle");
        buffer.insertText(0, "    x = 1\ny = 2");
        engine.eval("alle.active_window().goto_char(14)"); // y = 2 の行
        ScriptResult result = engine.eval("""
                from alle.modes.python.commands import PythonIndentState
                state = PythonIndentState(None)
                win = alle.active_window()
                state.cycle_indent(win, win.buffer(), 1)
                """);
        assertInstanceOf(ScriptResult.Success.class, result);
        assertTrue(buffer.getText().contains("    y = 2"), "前行と同じインデントに調整される");
    }

    @Test
    void newlineAndIndentでpass後にデデントされる() {
        engine.eval("import alle");
        buffer.insertText(0, "        pass");
        engine.eval("alle.active_window().goto_char(12)");
        ScriptResult result = engine.eval("""
                from alle.modes.python.commands import PythonIndentState
                state = PythonIndentState(None)
                win = alle.active_window()
                state.newline_and_indent(win, win.buffer())
                """);
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("        pass\n    ", buffer.getText());
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
