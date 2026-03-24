package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory.CreateResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KillLineCommandTest {

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

    private CreateResult createContextWithKillRing(
            String text, int point, KillRing killRing, Optional<String> lastCommand) {
        var buffer = new EditableBuffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        if (!text.isEmpty()) {
            window.insert(text);
        }
        window.setPoint(point);
        return new CreateResult(frame, TestCommandContextFactory.create(frame, bufferManager, killRing, lastCommand));
    }

    @Nested
    class 行末まで削除 {

        @Test
        void 行頭から行末まで削除する() {
            var result = createContext("Hello\nWorld", 0);
            new KillLineCommand().execute(result.context()).join();
            assertEquals("\nWorld", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 行中から行末まで削除する() {
            var result = createContext("Hello\nWorld", 2);
            new KillLineCommand().execute(result.context()).join();
            assertEquals(
                    "He\nWorld", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(2, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 改行のない行で行末まで削除する() {
            var result = createContext("Hello", 2);
            new KillLineCommand().execute(result.context()).join();
            assertEquals("He", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(2, result.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class 行末での改行削除 {

        @Test
        void 行末で改行を削除して次の行と結合する() {
            var result = createContext("Hello\nWorld", 5);
            new KillLineCommand().execute(result.context()).join();
            assertEquals(
                    "HelloWorld", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(5, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空行で改行を削除して次の行と結合する() {
            var result = createContext("Hello\n\nWorld", 6);
            new KillLineCommand().execute(result.context()).join();
            assertEquals(
                    "Hello\nWorld", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(6, result.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class バッファ末尾 {

        @Test
        void バッファ末尾では何もしない() {
            var result = createContext("Hello", 5);
            new KillLineCommand().execute(result.context()).join();
            assertEquals("Hello", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(5, result.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファでは何もしない() {
            var result = createContext("", 0);
            new KillLineCommand().execute(result.context()).join();
            assertEquals("", result.frame().getActiveWindow().getBuffer().getText());
            assertEquals(0, result.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class killRing蓄積 {

        @Test
        void 削除テキストがkillRingにpushされる() {
            var killRing = new KillRing();
            var result = createContextWithKillRing("Hello\nWorld", 0, killRing, Optional.empty());
            new KillLineCommand().execute(result.context()).join();
            assertEquals("Hello", killRing.current().orElseThrow());
        }

        @Test
        void 連続killでは前回エントリに追記される() {
            var killRing = new KillRing();
            // 1回目のkill
            var result1 = createContextWithKillRing("Hello\nWorld", 0, killRing, Optional.empty());
            new KillLineCommand().execute(result1.context()).join();
            assertEquals("Hello", killRing.current().orElseThrow());

            // 2回目のkill（lastCommandがkill-line）
            var result2 = createContextWithKillRing("\nWorld", 0, killRing, Optional.of("kill-line"));
            new KillLineCommand().execute(result2.context()).join();
            assertEquals("Hello\n", killRing.current().orElseThrow());
        }

        @Test
        void バッファ末尾ではkillRingに何も追加されない() {
            var killRing = new KillRing();
            var result = createContextWithKillRing("Hello", 5, killRing, Optional.empty());
            new KillLineCommand().execute(result.context()).join();
            assertTrue(killRing.current().isEmpty());
        }
    }
}
