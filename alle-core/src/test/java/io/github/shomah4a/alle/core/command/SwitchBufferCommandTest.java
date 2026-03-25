package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SwitchBufferCommandTest {

    private Frame frame;
    private BufferManager bufferManager;
    private BufferFacade scratchBuffer;
    private BufferFacade otherBuffer;

    @BeforeEach
    void setUp() {
        scratchBuffer = new BufferFacade(new EditableBuffer("*scratch*", new GapTextModel()));
        scratchBuffer.insertText(0, "scratch content");
        var window = new Window(scratchBuffer);
        var minibuffer = new Window(new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel())));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(scratchBuffer);

        otherBuffer = new BufferFacade(new EditableBuffer("other.txt", new GapTextModel()));
        otherBuffer.insertText(0, "other content");
        bufferManager.add(otherBuffer);
    }

    private InputPrompter confirming(String value) {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private InputPrompter confirmingAndCapture(String value, AtomicReference<String> capturedMessage) {
        return (message, history) -> {
            capturedMessage.set(message);
            return CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
        };
    }

    private InputPrompter cancelling() {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
    }

    @Nested
    class バッファ切り替え {

        @Test
        void 指定した名前のバッファに切り替わる() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("other.txt"));

            cmd.execute(context).join();

            assertEquals("other.txt", frame.getActiveWindow().getBuffer().getName());
            assertEquals("other content", frame.getActiveWindow().getBuffer().getText());
        }

        @Test
        void 切り替え後のカーソル位置が先頭になる() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("other.txt"));

            cmd.execute(context).join();

            assertEquals(0, frame.getActiveWindow().getPoint());
        }
    }

    @Nested
    class デフォルトバッファ {

        @Test
        void 直前バッファがある場合プロンプトにデフォルト名が表示される() {
            // まず other.txt に切り替えて previousBuffer を作る
            frame.getActiveWindow().setBuffer(otherBuffer);
            frame.getActiveWindow().setBuffer(scratchBuffer);

            var capturedMessage = new AtomicReference<>("");
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context =
                    TestCommandContextFactory.create(frame, bufferManager, confirmingAndCapture("", capturedMessage));

            cmd.execute(context).join();

            var message = Objects.requireNonNull(capturedMessage.get());
            assertTrue(message.contains("other.txt"));
        }

        @Test
        void 空入力時にデフォルトバッファに切り替わる() {
            // まず other.txt に切り替えて previousBuffer を作る
            frame.getActiveWindow().setBuffer(otherBuffer);
            frame.getActiveWindow().setBuffer(scratchBuffer);

            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming(""));

            cmd.execute(context).join();

            assertEquals("other.txt", frame.getActiveWindow().getBuffer().getName());
        }

        @Test
        void 直前バッファがない場合はデフォルトなしのプロンプトになる() {
            var capturedMessage = new AtomicReference<>("");
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context =
                    TestCommandContextFactory.create(frame, bufferManager, confirmingAndCapture("", capturedMessage));

            cmd.execute(context).join();

            assertEquals("Switch to buffer: ", Objects.requireNonNull(capturedMessage.get()));
        }

        @Test
        void 直前バッファがなく空入力の場合は何も変わらない() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming(""));

            cmd.execute(context).join();

            assertEquals("*scratch*", frame.getActiveWindow().getBuffer().getName());
        }
    }

    @Nested
    class 存在しないバッファ名 {

        @Test
        void 存在しないバッファ名で新規バッファに切り替わる() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("newbuf"));

            cmd.execute(context).join();

            assertEquals("newbuf", frame.getActiveWindow().getBuffer().getName());
        }

        @Test
        void 新規バッファの内容が空である() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("newbuf"));

            cmd.execute(context).join();

            assertEquals("", frame.getActiveWindow().getBuffer().getText());
        }

        @Test
        void 新規バッファにファイルパスが紐づいていない() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("newbuf"));

            cmd.execute(context).join();

            assertTrue(frame.getActiveWindow().getBuffer().getFilePath().isEmpty());
        }

        @Test
        void 新規バッファがBufferManagerに追加されている() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("newbuf"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("newbuf").isPresent());
        }

        @Test
        void 新規バッファ作成時にメッセージが表示される() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("newbuf"));

            cmd.execute(context).join();

            assertEquals(
                    "Buffer created: newbuf",
                    context.messageBuffer().getLastMessage().orElse(""));
        }

        @Test
        void 同じ名前で再度切り替えると既存バッファが使われる() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context1 = TestCommandContextFactory.create(frame, bufferManager, confirming("newbuf"));
            cmd.execute(context1).join();

            var created = bufferManager.findByName("newbuf").orElseThrow();
            int sizeAfterCreate = bufferManager.size();

            // scratchに戻してから再度newbufに切り替え
            frame.getActiveWindow().setBuffer(scratchBuffer);
            var context2 = TestCommandContextFactory.create(frame, bufferManager, confirming("newbuf"));
            cmd.execute(context2).join();

            assertEquals(sizeAfterCreate, bufferManager.size());
            assertEquals(created, frame.getActiveWindow().getBuffer());
        }
    }

    @Nested
    class キャンセル {

        @Test
        void キャンセル時は何も変わらない() {
            var cmd = new SwitchBufferCommand(new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, cancelling());

            cmd.execute(context).join();

            assertEquals("*scratch*", frame.getActiveWindow().getBuffer().getName());
        }
    }
}
