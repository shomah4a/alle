package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory.CreateResult;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SelfInsertCommandTest {

    private CreateResult createContext(KeyStroke triggeringKey) {
        var buffer = new EditableBuffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return new CreateResult(frame, TestCommandContextFactory.create(frame, bufferManager, triggeringKey));
    }

    @Nested
    class 文字挿入 {

        @Test
        void ASCII文字を挿入する() {
            var result = createContext(KeyStroke.of('a'));
            var cmd = new SelfInsertCommand();

            cmd.execute(result.context()).join();

            assertEquals("a", result.frame().getActiveWindow().getBuffer().getText());
        }

        @Test
        void 日本語文字を挿入する() {
            var result = createContext(KeyStroke.of('あ'));
            var cmd = new SelfInsertCommand();

            cmd.execute(result.context()).join();

            assertEquals("あ", result.frame().getActiveWindow().getBuffer().getText());
        }

        @Test
        void 絵文字を挿入する() {
            int codePoint = "😀".codePointAt(0);
            var result = createContext(KeyStroke.of(codePoint));
            var cmd = new SelfInsertCommand();

            cmd.execute(result.context()).join();

            assertEquals("😀", result.frame().getActiveWindow().getBuffer().getText());
        }
    }

    @Nested
    class 挿入しないケース {

        @Test
        void 修飾キー付きでは挿入しない() {
            var result = createContext(KeyStroke.ctrl('a'));
            var cmd = new SelfInsertCommand();

            cmd.execute(result.context()).join();

            assertEquals("", result.frame().getActiveWindow().getBuffer().getText());
        }

        @Test
        void triggeringKeyがemptyでは挿入しない() {
            var buffer = new EditableBuffer("test", new GapTextModel());
            var window = new Window(buffer);
            var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
            var frame = new Frame(window, minibuffer);
            var context = TestCommandContextFactory.create(frame, new BufferManager());
            var cmd = new SelfInsertCommand();

            cmd.execute(context).join();

            assertEquals("", buffer.getText());
        }
    }
}
