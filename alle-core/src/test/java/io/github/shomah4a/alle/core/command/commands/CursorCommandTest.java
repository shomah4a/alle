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

class CursorCommandTest {

    private CommandContext createContext(String initialText) {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        if (!initialText.isEmpty()) {
            window.insert(initialText);
            window.setPoint(0);
        }
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return TestCommandContextFactory.create(frame, bufferManager);
    }

    @Nested
    class ForwardChar {

        @Test
        void カーソルが1文字前方に移動する() {
            var context = createContext("Hello");
            var cmd = new ForwardCharCommand();

            cmd.execute(context).join();

            assertEquals(1, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 連続実行でカーソルが進む() {
            var context = createContext("Hello");
            var cmd = new ForwardCharCommand();

            cmd.execute(context).join();
            cmd.execute(context).join();
            cmd.execute(context).join();

            assertEquals(3, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 末尾ではカーソルが動かない() {
            var context = createContext("Hi");
            var cmd = new ForwardCharCommand();
            context.frame().getActiveWindow().setPoint(2);

            cmd.execute(context).join();

            assertEquals(2, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファではカーソルが動かない() {
            var context = createContext("");
            var cmd = new ForwardCharCommand();

            cmd.execute(context).join();

            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 絵文字をコードポイント単位で移動する() {
            var context = createContext("A😀B");
            var cmd = new ForwardCharCommand();

            cmd.execute(context).join();
            assertEquals(1, context.frame().getActiveWindow().getPoint());

            cmd.execute(context).join();
            assertEquals(2, context.frame().getActiveWindow().getPoint());

            cmd.execute(context).join();
            assertEquals(3, context.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class BackwardChar {

        @Test
        void カーソルが1文字後方に移動する() {
            var context = createContext("Hello");
            context.frame().getActiveWindow().setPoint(3);
            var cmd = new BackwardCharCommand();

            cmd.execute(context).join();

            assertEquals(2, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 先頭ではカーソルが動かない() {
            var context = createContext("Hello");
            var cmd = new BackwardCharCommand();

            cmd.execute(context).join();

            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 空バッファではカーソルが動かない() {
            var context = createContext("");
            var cmd = new BackwardCharCommand();

            cmd.execute(context).join();

            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }
    }
}
