package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Direction;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KillBufferCommandTest {

    private Buffer scratch;
    private Buffer fooBuffer;
    private Frame frame;
    private BufferManager bufferManager;
    private final KillBufferCommand cmd = new KillBufferCommand();

    @BeforeEach
    void setUp() {
        scratch = new EditableBuffer("*scratch*", new GapTextModel());
        fooBuffer = new EditableBuffer("foo.txt", new GapTextModel());
        var window = new Window(scratch);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(scratch);
        bufferManager.add(fooBuffer);
    }

    private InputPrompter confirming(String value) {
        return message -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private InputPrompter cancelling() {
        return message -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
    }

    @Nested
    class 基本動作 {

        @Test
        void 指定バッファが削除される() {
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("foo.txt"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("foo.txt").isEmpty());
            assertEquals(1, bufferManager.size());
        }

        @Test
        void 空入力でデフォルトの現在バッファが削除される() {
            frame.getActiveWindow().setBuffer(fooBuffer);
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming(""));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("foo.txt").isEmpty());
        }

        @Test
        void キャンセル時は何も変わらない() {
            var context = TestCommandContextFactory.create(frame, bufferManager, cancelling());

            cmd.execute(context).join();

            assertEquals(2, bufferManager.size());
        }

        @Test
        void 存在しないバッファ名では何も変わらない() {
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("nonexistent"));

            cmd.execute(context).join();

            assertEquals(2, bufferManager.size());
        }
    }

    @Nested
    class 最後のバッファ {

        @Test
        void 最後の1つのバッファは削除できない() {
            bufferManager.remove("foo.txt");
            assertEquals(1, bufferManager.size());

            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*scratch*"));

            cmd.execute(context).join();

            assertEquals(1, bufferManager.size());
            assertTrue(bufferManager.findByName("*scratch*").isPresent());
        }
    }

    @Nested
    class ウィンドウの切り替え {

        @Test
        void 削除対象を表示中のウィンドウが別バッファに切り替わる() {
            frame.getActiveWindow().setBuffer(fooBuffer);
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("foo.txt"));

            cmd.execute(context).join();

            assertNotSame(fooBuffer, frame.getActiveWindow().getBuffer());
        }

        @Test
        void 非アクティブウィンドウが削除対象を表示中の場合も切り替わる() {
            // ウィンドウを分割して両方にfooBufferを表示
            var barBuffer = new EditableBuffer("bar.txt", new GapTextModel());
            bufferManager.add(barBuffer);
            frame.splitActiveWindow(Direction.VERTICAL, barBuffer);
            // 最初のウィンドウ(scratch)をfooに変更
            var windows = frame.getWindowTree().windows();
            windows.get(0).setBuffer(fooBuffer);

            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("foo.txt"));

            cmd.execute(context).join();

            for (var window : frame.getWindowTree().windows()) {
                assertNotSame(fooBuffer, window.getBuffer());
            }
        }

        @Test
        void 他のウィンドウで表示されていないバッファに優先的に切り替わる() {
            var barBuffer = new EditableBuffer("bar.txt", new GapTextModel());
            bufferManager.add(barBuffer);
            // window1: scratch, window2: fooBuffer
            frame.splitActiveWindow(Direction.VERTICAL, fooBuffer);
            // scratchを削除 → barが他ウィンドウで表示されていないので優先
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*scratch*"));

            cmd.execute(context).join();

            // scratch削除後に再作成されるので、barかscratchが切り替え先
            var windows = frame.getWindowTree().windows();
            var switchedWindow = windows.detect(w -> w.getBuffer() != fooBuffer);
            // barが他ウィンドウで表示されていないので優先される
            assertSame(barBuffer, switchedWindow.getBuffer());
        }
    }

    @Nested
    class scratchバッファの再作成 {

        @Test
        void scratch削除後にサイレントで再作成される() {
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*scratch*"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("*scratch*").isPresent());
        }

        @Test
        void 再作成されたscratchは新しいインスタンスである() {
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*scratch*"));

            cmd.execute(context).join();

            var newScratch = bufferManager.findByName("*scratch*").orElseThrow();
            assertNotSame(scratch, newScratch);
        }

        @Test
        void scratch削除時に別バッファがあればそちらに切り替わる() {
            // 現在scratchを表示中 → 削除すると fooBuffer に切り替わる
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*scratch*"));

            cmd.execute(context).join();

            assertSame(fooBuffer, frame.getActiveWindow().getBuffer());
        }
    }

    @Nested
    class previousBufferのクリーンアップ {

        @Test
        void 削除対象がpreviousBufferの場合にクリアされる() {
            // scratch → foo に切り替え → scratch が previousBuffer になる
            frame.getActiveWindow().setBuffer(fooBuffer);
            assertSame(scratch, frame.getActiveWindow().getPreviousBuffer().orElseThrow());

            // scratch を削除
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*scratch*"));
            cmd.execute(context).join();

            // previousBuffer に元の scratch が残っていないこと
            // (setBufferで切り替えが発生するのでpreviousBufferは更新される)
            var prevOpt = frame.getActiveWindow().getPreviousBuffer();
            if (prevOpt.isPresent()) {
                assertNotSame(scratch, prevOpt.get());
            }
        }
    }
}
