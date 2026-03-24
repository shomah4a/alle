package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory.CreateResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommandTest {

    private CreateResult createContext() {
        var buffer = new EditableBuffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return new CreateResult(frame, TestCommandContextFactory.create(frame, bufferManager));
    }

    @Nested
    class コマンド実行 {

        @Test
        void コマンドがコンテキスト経由でバッファを操作できる() {
            var result = createContext();
            var context = result.context();
            var frame = result.frame();
            Command insertHello = new Command() {
                @Override
                public String name() {
                    return "insert-hello";
                }

                @Override
                public CompletableFuture<Void> execute(CommandContext ctx) {
                    frame.getActiveWindow().insert("Hello");
                    return CompletableFuture.completedFuture(null);
                }
            };

            insertHello.execute(context).join();

            assertEquals("Hello", frame.getActiveWindow().getBuffer().getText());
            assertEquals(5, frame.getActiveWindow().getPoint());
        }

        @Test
        void コマンド名を取得できる() {
            Command noop = new Command() {
                @Override
                public String name() {
                    return "noop";
                }

                @Override
                public CompletableFuture<Void> execute(CommandContext ctx) {
                    return CompletableFuture.completedFuture(null);
                }
            };

            assertEquals("noop", noop.name());
        }
    }

    @Nested
    class コンテキスト {

        @Test
        void フレームとバッファマネージャにアクセスできる() {
            var result = createContext();
            assertEquals("test", result.frame().getActiveWindow().getBuffer().getName());
            assertEquals(1, result.context().bufferManager().size());
        }
    }
}
