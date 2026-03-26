package io.github.shomah4a.alle.script;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditorFacadeTest {

    private EditorFacade facade;
    private TextBuffer buffer;
    private MessageBuffer messageBuffer;

    @BeforeEach
    void setUp() {
        buffer = new TextBuffer("test.py", new GapTextModel(), new SettingsRegistry());
        var bufferFacade = new BufferFacade(buffer);
        var window = new Window(bufferFacade);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(bufferFacade);
        messageBuffer = new MessageBuffer("*Messages*", 100, new SettingsRegistry());
        facade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new));
    }

    @Test
    void アクティブウィンドウのファサードを取得できる() {
        WindowFacade win = facade.activeWindow();
        assertEquals(0, win.point());
    }

    @Test
    void カレントバッファのファサードを取得できる() {
        io.github.shomah4a.alle.script.BufferFacade buf = facade.currentBuffer();
        assertEquals("test.py", buf.name());
    }

    @Test
    void ウィンドウファサード経由でテキストを挿入できる() {
        WindowFacade win = facade.activeWindow();
        win.insert("hello");
        assertEquals("hello", buffer.getText());
    }

    @Test
    void ウィンドウファサード経由でカーソルを移動できる() {
        WindowFacade win = facade.activeWindow();
        win.insert("hello world");
        win.gotoChar(5);
        assertEquals(5, win.point());
    }

    @Test
    void バッファファサード経由で行数を取得できる() {
        buffer.insertText(0, "line1\nline2\nline3");
        io.github.shomah4a.alle.script.BufferFacade buf = facade.currentBuffer();
        assertEquals(3, buf.lineCount());
    }

    @Test
    void メッセージを表示できる() {
        facade.message("test message");
        assertEquals("test message", messageBuffer.lineText(0));
    }
}
