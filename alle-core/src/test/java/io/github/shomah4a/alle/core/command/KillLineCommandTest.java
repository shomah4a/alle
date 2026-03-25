package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KillLineCommandTest {

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

    private CommandContext createContextWithKillRing(
            String text, int point, KillRing killRing, Optional<String> lastCommand) {
        var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel()));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel())));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        if (!text.isEmpty()) {
            window.insert(text);
        }
        window.setPoint(point);
        return TestCommandContextFactory.create(frame, bufferManager, killRing, lastCommand);
    }

    @Nested
    class 行末まで削除 {

        @Test
        void 行頭から行末まで削除する() {
            var context = createContext("Hello\nWorld", 0);
            new KillLineCommand().execute(context).join();
            assertEquals(
                    "\nWorld", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 行中から行末まで削除する() {
            var context = createContext("Hello\nWorld", 2);
            new KillLineCommand().execute(context).join();
            assertEquals(
                    "He\nWorld", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(2, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 改行のない行で行末まで削除する() {
            var context = createContext("Hello", 2);
            new KillLineCommand().execute(context).join();
            assertEquals("He", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(2, context.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class 行末での改行削除 {

        @Test
        void 行末で改行を削除して次の行と結合する() {
            var context = createContext("Hello\nWorld", 5);
            new KillLineCommand().execute(context).join();
            assertEquals(
                    "HelloWorld", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空行で改行を削除して次の行と結合する() {
            var context = createContext("Hello\n\nWorld", 6);
            new KillLineCommand().execute(context).join();
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
            new KillLineCommand().execute(context).join();
            assertEquals("Hello", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファでは何もしない() {
            var context = createContext("", 0);
            new KillLineCommand().execute(context).join();
            assertEquals("", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class killRing蓄積 {

        @Test
        void 削除テキストがkillRingにpushされる() {
            var killRing = new KillRing();
            var context = createContextWithKillRing("Hello\nWorld", 0, killRing, Optional.empty());
            new KillLineCommand().execute(context).join();
            assertEquals("Hello", killRing.current().orElseThrow());
        }

        @Test
        void 連続killでは前回エントリに追記される() {
            var killRing = new KillRing();
            // 1回目のkill
            var context1 = createContextWithKillRing("Hello\nWorld", 0, killRing, Optional.empty());
            new KillLineCommand().execute(context1).join();
            assertEquals("Hello", killRing.current().orElseThrow());

            // 2回目のkill（lastCommandがkill-line）
            var context2 = createContextWithKillRing("\nWorld", 0, killRing, Optional.of("kill-line"));
            new KillLineCommand().execute(context2).join();
            assertEquals("Hello\n", killRing.current().orElseThrow());
        }

        @Test
        void バッファ末尾ではkillRingに何も追加されない() {
            var killRing = new KillRing();
            var context = createContextWithKillRing("Hello", 5, killRing, Optional.empty());
            new KillLineCommand().execute(context).join();
            assertTrue(killRing.current().isEmpty());
        }
    }
}
