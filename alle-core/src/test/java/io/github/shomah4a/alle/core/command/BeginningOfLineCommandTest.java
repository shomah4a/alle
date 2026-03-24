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

class BeginningOfLineCommandTest {

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
    class 行頭移動 {

        @Test
        void 行中から行頭に移動する() {
            var result = createContext("Hello", 3);
            new BeginningOfLineCommand().execute(result.context()).join();
            assertEquals(0, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 行頭にいる場合は移動しない() {
            var result = createContext("Hello", 0);
            new BeginningOfLineCommand().execute(result.context()).join();
            assertEquals(0, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 複数行の2行目で行頭に移動する() {
            var result = createContext("Hello\nWorld", 8);
            new BeginningOfLineCommand().execute(result.context()).join();
            assertEquals(6, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 改行文字上から行頭に移動する() {
            var result = createContext("Hello\nWorld", 5);
            new BeginningOfLineCommand().execute(result.context()).join();
            assertEquals(0, result.frame().getActiveWindow().getPoint());
        }
    }
}
