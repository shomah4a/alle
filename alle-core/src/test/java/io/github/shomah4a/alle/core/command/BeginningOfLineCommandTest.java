package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BeginningOfLineCommandTest {

    private CommandContext createContext(String text, int point) {
        var buffer = new EditableBuffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        window.setPoint(point);
        return TestCommandContextFactory.create(frame, new BufferManager());
    }

    @Nested
    class 行頭移動 {

        @Test
        void 行中から行頭に移動する() {
            var context = createContext("Hello", 3);
            new BeginningOfLineCommand().execute(context).join();
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 行頭にいる場合は移動しない() {
            var context = createContext("Hello", 0);
            new BeginningOfLineCommand().execute(context).join();
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 複数行の2行目で行頭に移動する() {
            var context = createContext("Hello\nWorld", 8);
            new BeginningOfLineCommand().execute(context).join();
            assertEquals(6, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 改行文字上から行頭に移動する() {
            var context = createContext("Hello\nWorld", 5);
            new BeginningOfLineCommand().execute(context).join();
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }
    }
}
