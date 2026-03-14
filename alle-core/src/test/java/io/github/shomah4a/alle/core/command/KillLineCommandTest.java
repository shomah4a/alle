package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KillLineCommandTest {

    private CommandContext createContext(String text, int point) {
        var buffer = new Buffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new Buffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        window.setPoint(point);
        return TestCommandContextFactory.create(frame, new BufferManager());
    }

    @Nested
    class 行末まで削除 {

        @Test
        void 行頭から行末まで削除する() {
            var context = createContext("Hello\nWorld", 0);
            new KillLineCommand().execute(context);
            assertEquals(
                    "\nWorld", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 行中から行末まで削除する() {
            var context = createContext("Hello\nWorld", 2);
            new KillLineCommand().execute(context);
            assertEquals(
                    "He\nWorld", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(2, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 改行のない行で行末まで削除する() {
            var context = createContext("Hello", 2);
            new KillLineCommand().execute(context);
            assertEquals("He", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(2, context.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class 行末での改行削除 {

        @Test
        void 行末で改行を削除して次の行と結合する() {
            var context = createContext("Hello\nWorld", 5);
            new KillLineCommand().execute(context);
            assertEquals(
                    "HelloWorld", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空行で改行を削除して次の行と結合する() {
            var context = createContext("Hello\n\nWorld", 6);
            new KillLineCommand().execute(context);
            assertEquals(
                    "Hello\nWorld",
                    context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(6, context.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class バッファ末尾 {

        @Test
        void バッファ末尾では何もしない() {
            var context = createContext("Hello", 5);
            new KillLineCommand().execute(context);
            assertEquals("Hello", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファでは何もしない() {
            var context = createContext("", 0);
            new KillLineCommand().execute(context);
            assertEquals("", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }
    }
}
