package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EndOfLineCommandTest {

    private CommandContext createContext(String text, int point) {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        var frame = new Frame(window, minibuffer);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        window.setPoint(point);
        return TestCommandContextFactory.create(frame, new BufferManager());
    }

    @Nested
    class 行末移動 {

        @Test
        void 行頭から行末に移動する() {
            var context = createContext("Hello", 0);
            new EndOfLineCommand().execute(context).join();
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 行末にいる場合は移動しない() {
            var context = createContext("Hello", 5);
            new EndOfLineCommand().execute(context).join();
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 複数行の1行目で行末に移動する() {
            var context = createContext("Hello\nWorld", 2);
            new EndOfLineCommand().execute(context).join();
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 複数行の2行目で行末に移動する() {
            var context = createContext("Hello\nWorld", 6);
            new EndOfLineCommand().execute(context).join();
            assertEquals(11, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 改行文字上から行末に移動すると改行位置に留まる() {
            // 改行文字はその行の末尾として扱われるので、行末=改行位置
            var context = createContext("Hello\nWorld", 5);
            new EndOfLineCommand().execute(context).join();
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }
    }
}
