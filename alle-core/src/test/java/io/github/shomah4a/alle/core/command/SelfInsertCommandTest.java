package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SelfInsertCommandTest {

    private CommandContext createContext(KeyStroke triggeringKey) {
        var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        var minibuffer = new Window(
                new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return TestCommandContextFactory.create(frame, bufferManager, triggeringKey);
    }

    @Nested
    class 文字挿入 {

        @Test
        void ASCII文字を挿入する() {
            var context = createContext(KeyStroke.of('a'));
            var cmd = new SelfInsertCommand();

            cmd.execute(context).join();

            assertEquals("a", context.frame().getActiveWindow().getBuffer().getText());
        }

        @Test
        void 日本語文字を挿入する() {
            var context = createContext(KeyStroke.of('あ'));
            var cmd = new SelfInsertCommand();

            cmd.execute(context).join();

            assertEquals("あ", context.frame().getActiveWindow().getBuffer().getText());
        }

        @Test
        void 絵文字を挿入する() {
            int codePoint = "😀".codePointAt(0);
            var context = createContext(KeyStroke.of(codePoint));
            var cmd = new SelfInsertCommand();

            cmd.execute(context).join();

            assertEquals("😀", context.frame().getActiveWindow().getBuffer().getText());
        }
    }

    @Nested
    class 挿入しないケース {

        @Test
        void 修飾キー付きでは挿入しない() {
            var context = createContext(KeyStroke.ctrl('a'));
            var cmd = new SelfInsertCommand();

            cmd.execute(context).join();

            assertEquals("", context.frame().getActiveWindow().getBuffer().getText());
        }

        @Test
        void triggeringKeyがemptyでは挿入しない() {
            var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel(), new SettingsRegistry()));
            var window = new Window(buffer);
            var minibuffer = new Window(
                    new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
            var frame = new Frame(window, minibuffer);
            var context = TestCommandContextFactory.create(frame, new BufferManager());
            var cmd = new SelfInsertCommand();

            cmd.execute(context).join();

            assertEquals("", buffer.getText());
        }
    }
}
