package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Test;

class MarkWholeBufferCommandTest {

    private CommandContext createContext(String initialText) {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        if (!initialText.isEmpty()) {
            window.insert(initialText);
            window.setPoint(0);
        }
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return TestCommandContextFactory.create(frame, bufferManager);
    }

    @Test
    void 空バッファではpointとmarkが共に0になる() {
        var context = createContext("");
        var cmd = new MarkWholeBufferCommand();

        cmd.execute(context).join();

        var window = context.frame().getActiveWindow();
        assertEquals(0, window.getPoint());
        assertTrue(window.getMark().isPresent());
        assertEquals(0, window.getMark().get());
    }

    @Test
    void バッファ全体が選択されpointが先頭でmarkが末尾になる() {
        var context = createContext("Hello World");
        var cmd = new MarkWholeBufferCommand();

        cmd.execute(context).join();

        var window = context.frame().getActiveWindow();
        assertEquals(0, window.getPoint());
        assertEquals(11, window.getMark().get());
    }

    @Test
    void 複数行テキストでリージョンがバッファ全体を覆う() {
        var context = createContext("line1\nline2\nline3");
        var cmd = new MarkWholeBufferCommand();

        cmd.execute(context).join();

        var window = context.frame().getActiveWindow();
        assertEquals(0, window.getRegionStart().get());
        assertEquals(17, window.getRegionEnd().get());
    }

    @Test
    void カーソルが途中にある状態から実行してもバッファ全体が選択される() {
        var context = createContext("abcdef");
        var window = context.frame().getActiveWindow();
        window.setPoint(3);

        var cmd = new MarkWholeBufferCommand();
        cmd.execute(context).join();

        assertEquals(0, window.getPoint());
        assertEquals(6, window.getMark().get());
    }

    @Test
    void keepsRegionActiveがtrueを返す() {
        var cmd = new MarkWholeBufferCommand();
        assertTrue(cmd.keepsRegionActive());
    }
}
