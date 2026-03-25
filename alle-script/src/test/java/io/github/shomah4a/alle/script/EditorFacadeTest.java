package io.github.shomah4a.alle.script;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditorFacadeTest {

    private EditorFacade facade;
    private EditableBuffer buffer;
    private MessageBuffer messageBuffer;

    @BeforeEach
    void setUp() {
        buffer = new EditableBuffer("test.py", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        messageBuffer = new MessageBuffer("*Messages*", 100);
        facade = new EditorFacade(frame, messageBuffer, new CommandRegistry(), new Keymap("global"));
    }

    @Test
    void アクティブウィンドウのファサードを取得できる() {
        WindowFacade win = facade.activeWindow();
        assertEquals(0, win.point().join());
    }

    @Test
    void カレントバッファのファサードを取得できる() {
        BufferFacade buf = facade.currentBuffer();
        assertEquals("test.py", buf.name());
    }

    @Test
    void ウィンドウファサード経由でテキストを挿入できる() {
        WindowFacade win = facade.activeWindow();
        win.insert("hello").join();
        assertEquals("hello", buffer.getText());
    }

    @Test
    void ウィンドウファサード経由でカーソルを移動できる() {
        WindowFacade win = facade.activeWindow();
        win.insert("hello world").join();
        win.gotoChar(5).join();
        assertEquals(5, win.point().join());
    }

    @Test
    void バッファファサード経由で行数を取得できる() {
        buffer.insertText(0, "line1\nline2\nline3");
        BufferFacade buf = facade.currentBuffer();
        assertEquals(3, buf.lineCount());
    }

    @Test
    void メッセージを表示できる() {
        facade.message("test message");
        assertEquals("test message", messageBuffer.lineText(0));
    }
}
