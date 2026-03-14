package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommandTest {

    private CommandContext createContext() {
        var buffer = new Buffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new Buffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return new CommandContext(frame, bufferManager);
    }

    @Nested
    class コマンド実行 {

        @Test
        void コマンドがコンテキスト経由でバッファを操作できる() {
            var context = createContext();
            Command insertHello = new Command() {
                @Override
                public String name() {
                    return "insert-hello";
                }

                @Override
                public void execute(CommandContext ctx) {
                    ctx.frame().getActiveWindow().insert("Hello");
                }
            };

            insertHello.execute(context);

            assertEquals("Hello", context.frame().getActiveWindow().getBuffer().getText());
            assertEquals(5, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void コマンド名を取得できる() {
            Command noop = new Command() {
                @Override
                public String name() {
                    return "noop";
                }

                @Override
                public void execute(CommandContext ctx) {}
            };

            assertEquals("noop", noop.name());
        }
    }

    @Nested
    class コンテキスト {

        @Test
        void フレームとバッファマネージャにアクセスできる() {
            var context = createContext();
            assertEquals("test", context.frame().getActiveWindow().getBuffer().getName());
            assertEquals(1, context.bufferManager().size());
        }
    }
}
