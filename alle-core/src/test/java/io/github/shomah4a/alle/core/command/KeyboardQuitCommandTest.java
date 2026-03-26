package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeyboardQuitCommandTest {

    private Frame frame;
    private Window mainWindow;
    private BufferManager bufferManager;
    private final KeyboardQuitCommand cmd = new KeyboardQuitCommand();

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        mainWindow = new Window(buffer);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        frame = new Frame(mainWindow, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
    }

    @Test
    void マークが解除される() {
        mainWindow.getBuffer().insertText(0, "hello world");
        mainWindow.setMark(5);
        assertTrue(mainWindow.getMark().isPresent());

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertTrue(mainWindow.getMark().isEmpty());
    }

    @Test
    void エコーエリアにQuitが表示される() {
        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertTrue(context.messageBuffer().isShowingMessage());
        assertEquals("Quit", context.messageBuffer().getLastMessage().orElse(""));
    }

    @Test
    void マーク未設定でも正常に動作する() {
        assertTrue(mainWindow.getMark().isEmpty());

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertTrue(mainWindow.getMark().isEmpty());
        assertEquals("Quit", context.messageBuffer().getLastMessage().orElse(""));
    }
}
