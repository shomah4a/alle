package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SwitchBufferCommandTest {

    private Frame frame;
    private BufferManager bufferManager;
    private Buffer scratchBuffer;
    private Buffer otherBuffer;

    @BeforeEach
    void setUp() {
        scratchBuffer = new Buffer("*scratch*", new GapTextModel());
        scratchBuffer.insertText(0, "scratch content");
        var window = new Window(scratchBuffer);
        var minibuffer = new Window(new Buffer("*Minibuffer*", new GapTextModel()));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(scratchBuffer);

        otherBuffer = new Buffer("other.txt", new GapTextModel());
        otherBuffer.insertText(0, "other content");
        bufferManager.add(otherBuffer);
    }

    private InputPrompter confirming(String value) {
        return message -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private InputPrompter cancelling() {
        return message -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
    }

    @Nested
    class バッファ切り替え {

        @Test
        void 指定した名前のバッファに切り替わる() {
            var cmd = new SwitchBufferCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("other.txt"));

            cmd.execute(context).join();

            assertEquals("other.txt", frame.getActiveWindow().getBuffer().getName());
            assertEquals("other content", frame.getActiveWindow().getBuffer().getText());
        }

        @Test
        void 切り替え後のカーソル位置が先頭になる() {
            var cmd = new SwitchBufferCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("other.txt"));

            cmd.execute(context).join();

            assertEquals(0, frame.getActiveWindow().getPoint());
        }
    }

    @Nested
    class 存在しないバッファ名 {

        @Test
        void 存在しないバッファ名では何も変わらない() {
            var cmd = new SwitchBufferCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("nonexistent"));

            cmd.execute(context).join();

            assertEquals("*scratch*", frame.getActiveWindow().getBuffer().getName());
        }
    }

    @Nested
    class キャンセル {

        @Test
        void キャンセル時は何も変わらない() {
            var cmd = new SwitchBufferCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager, cancelling());

            cmd.execute(context).join();

            assertEquals("*scratch*", frame.getActiveWindow().getBuffer().getName());
        }
    }

    @Nested
    class 空文字列入力 {

        @Test
        void 空文字列では何も変わらない() {
            var cmd = new SwitchBufferCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming(""));

            cmd.execute(context).join();

            assertEquals("*scratch*", frame.getActiveWindow().getBuffer().getName());
        }
    }
}
