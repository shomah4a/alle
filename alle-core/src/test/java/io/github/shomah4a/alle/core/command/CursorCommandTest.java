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

class CursorCommandTest {

    private CreateResult createContext(String initialText) {
        var buffer = new EditableBuffer("test", new GapTextModel());
        var window = new Window(buffer);
        if (!initialText.isEmpty()) {
            window.insert(initialText);
            window.setPoint(0);
        }
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return new CreateResult(frame, TestCommandContextFactory.create(frame, bufferManager));
    }

    @Nested
    class ForwardChar {

        @Test
        void カーソルが1文字前方に移動する() {
            var result = createContext("Hello");
            var cmd = new ForwardCharCommand();

            cmd.execute(result.context()).join();

            assertEquals(1, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 連続実行でカーソルが進む() {
            var result = createContext("Hello");
            var cmd = new ForwardCharCommand();

            cmd.execute(result.context()).join();
            cmd.execute(result.context()).join();
            cmd.execute(result.context()).join();

            assertEquals(3, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 末尾ではカーソルが動かない() {
            var result = createContext("Hi");
            var cmd = new ForwardCharCommand();
            result.frame().getActiveWindow().setPoint(2);

            cmd.execute(result.context()).join();

            assertEquals(2, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファではカーソルが動かない() {
            var result = createContext("");
            var cmd = new ForwardCharCommand();

            cmd.execute(result.context()).join();

            assertEquals(0, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 絵文字をコードポイント単位で移動する() {
            var result = createContext("A😀B");
            var cmd = new ForwardCharCommand();

            cmd.execute(result.context()).join();
            assertEquals(1, result.frame().getActiveWindow().getPoint());

            cmd.execute(result.context()).join();
            assertEquals(2, result.frame().getActiveWindow().getPoint());

            cmd.execute(result.context()).join();
            assertEquals(3, result.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class BackwardChar {

        @Test
        void カーソルが1文字後方に移動する() {
            var result = createContext("Hello");
            result.frame().getActiveWindow().setPoint(3);
            var cmd = new BackwardCharCommand();

            cmd.execute(result.context()).join();

            assertEquals(2, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 先頭ではカーソルが動かない() {
            var result = createContext("Hello");
            var cmd = new BackwardCharCommand();

            cmd.execute(result.context()).join();

            assertEquals(0, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファではカーソルが動かない() {
            var result = createContext("");
            var cmd = new BackwardCharCommand();

            cmd.execute(result.context()).join();

            assertEquals(0, result.frame().getActiveWindow().getPoint());
        }
    }
}
