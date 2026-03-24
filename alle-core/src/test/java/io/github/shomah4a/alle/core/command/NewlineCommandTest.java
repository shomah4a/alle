package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory.CreateResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NewlineCommandTest {

    private CreateResult createContext(String text, int point) {
        var buffer = new EditableBuffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        window.setPoint(point);
        return new CreateResult(frame, TestCommandContextFactory.create(frame, new BufferManager()));
    }

    @Nested
    class 改行挿入 {

        @Test
        void カーソル位置に改行を挿入する() {
            var result = createContext("HelloWorld", 5);
            new NewlineCommand().execute(result.context()).join();
            assertEquals(
                    "Hello\nWorld", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(6, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 先頭に改行を挿入する() {
            var result = createContext("Hello", 0);
            new NewlineCommand().execute(result.context()).join();
            assertEquals("\nHello", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(1, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 末尾に改行を挿入する() {
            var result = createContext("Hello", 5);
            new NewlineCommand().execute(result.context()).join();
            assertEquals("Hello\n", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(6, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファに改行を挿入する() {
            var result = createContext("", 0);
            new NewlineCommand().execute(result.context()).join();
            assertEquals("\n", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(1, result.frame().getActiveWindow().getPoint());
        }
    }
}
