package io.github.shomah4a.alle.script.graalpy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
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
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
        messageBuffer = new MessageBuffer("*Messages*", 100);

        var facade = new EditorFacade(frame, messageBuffer, new CommandRegistry(), new Keymap("global"));
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
        ScriptResult result = engine.eval("alle.active_window().point().result()");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("0", ((ScriptResult.Success) result).value());
    }

    @Test
    void alleモジュール経由でテキストを挿入できる() {
        engine.eval("import alle");
        engine.eval("alle.active_window().insert('hello').result()");
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
    void asyncio_runでawaitを使ってテキスト挿入できる() {
        engine.eval("import alle");
        engine.eval("import asyncio");
        engine.eval("""
                async def test():
                    win = alle.active_window()
                    await win.insert('async hello')
                    pos = await win.point()
                    return pos
                """);
        ScriptResult result = engine.eval("asyncio.run(test())");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("async hello", buffer.getText());
    }

    @Test
    void awaitでカーソル位置を取得できる() {
        buffer.insertText(0, "hello world");
        engine.eval("import alle");
        engine.eval("import asyncio");
        engine.eval("""
                async def test():
                    win = alle.active_window()
                    await win.goto_char(5)
                    return await win.point()
                """);
        ScriptResult result = engine.eval("asyncio.run(test())");
        assertInstanceOf(ScriptResult.Success.class, result);
        assertEquals("5", ((ScriptResult.Success) result).value());
    }

    @Test
    void printの出力がstdoutバッファに記録される() {
        engine.eval("print('hello from python')");
        var stdoutBuffer =
                (MessageBuffer) bufferManager.findByName("*Python Output*").orElseThrow();
        assertEquals("hello from python", stdoutBuffer.lineText(0));
    }

    @Test
    void 複数回のprintが個別に記録される() {
        engine.eval("print('line1')");
        engine.eval("print('line2')");
        var stdoutBuffer =
                (MessageBuffer) bufferManager.findByName("*Python Output*").orElseThrow();
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
}
