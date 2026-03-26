package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NewlineCommandTest {

    private CommandContext createContext(String text, int point) {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        var frame = new Frame(window, minibuffer);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        window.setPoint(point);
        return TestCommandContextFactory.create(frame, new BufferManager());
    }

    @Nested
    class 改行挿入 {

        @Test
        void カーソル位置に改行を挿入する() {
            var context = createContext("HelloWorld", 5);
            new NewlineCommand().execute(context).join();
            assertEquals(
                    "Hello\nWorld",
                    context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(6, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 先頭に改行を挿入する() {
            var context = createContext("Hello", 0);
            new NewlineCommand().execute(context).join();
            assertEquals(
                    "\nHello", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(1, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 末尾に改行を挿入する() {
            var context = createContext("Hello", 5);
            new NewlineCommand().execute(context).join();
            assertEquals(
                    "Hello\n", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(6, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファに改行を挿入する() {
            var context = createContext("", 0);
            new NewlineCommand().execute(context).join();
            assertEquals("\n", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(1, context.frame().getActiveWindow().getPoint());
        }
    }
}
