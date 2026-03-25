package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BackwardDeleteCharCommandTest {

    private CommandContext createContext(String text, int point) {
        var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel()));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel())));
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
        void カーソル前の文字を削除する() {
            var context = createContext("Hello", 1);
            new BackwardDeleteCharCommand().execute(context).join();
            assertEquals("ello", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 中間位置でカーソル前の文字を削除する() {
            var context = createContext("Hello", 3);
            new BackwardDeleteCharCommand().execute(context).join();
            assertEquals("Helo", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(2, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void バッファ先頭では何もしない() {
            var context = createContext("Hello", 0);
            new BackwardDeleteCharCommand().execute(context).join();
            assertEquals("Hello", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }
    }
}
