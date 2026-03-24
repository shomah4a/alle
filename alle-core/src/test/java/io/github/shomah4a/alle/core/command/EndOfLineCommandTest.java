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

class EndOfLineCommandTest {

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
    class 行末移動 {

        @Test
        void 行頭から行末に移動する() {
            var result = createContext("Hello", 0);
            new EndOfLineCommand().execute(result.context()).join();
            assertEquals(5, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 行末にいる場合は移動しない() {
            var result = createContext("Hello", 5);
            new EndOfLineCommand().execute(result.context()).join();
            assertEquals(5, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 複数行の1行目で行末に移動する() {
            var result = createContext("Hello\nWorld", 2);
            new EndOfLineCommand().execute(result.context()).join();
            assertEquals(5, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 複数行の2行目で行末に移動する() {
            var result = createContext("Hello\nWorld", 6);
            new EndOfLineCommand().execute(result.context()).join();
            assertEquals(11, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 改行文字上から行末に移動すると改行位置に留まる() {
            // 改行文字はその行の末尾として扱われるので、行末=改行位置
            var result = createContext("Hello\nWorld", 5);
            new EndOfLineCommand().execute(result.context()).join();
            assertEquals(5, result.frame().getActiveWindow().getPoint());
        }
    }
}
