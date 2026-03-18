package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DeleteCharCommandTest {

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
    class 文字削除 {

        @Test
        void カーソル位置の文字を削除する() {
            var context = createContext("Hello", 0);
            new DeleteCharCommand().execute(context);
            assertEquals("ello", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 中間位置の文字を削除する() {
            var context = createContext("Hello", 2);
            new DeleteCharCommand().execute(context);
            assertEquals("Helo", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(2, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void バッファ末尾では何もしない() {
            var context = createContext("Hello", 5);
            new DeleteCharCommand().execute(context);
            assertEquals("Hello", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }
    }
}
