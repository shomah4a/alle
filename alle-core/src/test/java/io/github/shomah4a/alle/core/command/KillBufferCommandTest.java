package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Direction;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KillBufferCommandTest {

    private BufferFacade scratch;
    private BufferFacade fooBuffer;
    private Frame frame;
    private BufferManager bufferManager;
    private final MutableMap<String, StringWriter> writerStorage = Maps.mutable.empty();
    private BufferIO bufferIO;
    private KillBufferCommand cmd;

    @BeforeEach
    void setUp() {
        scratch = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), new SettingsRegistry()));
        fooBuffer = new BufferFacade(new TextBuffer("foo.txt", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(scratch);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(scratch);
        bufferManager.add(fooBuffer);

        bufferIO = new BufferIO(
                source -> new StringReader(""),
                destination -> {
                    var sw = new StringWriter();
                    writerStorage.put(destination, sw);
                    return sw;
                },
                new SettingsRegistry());
        cmd = new KillBufferCommand(new InputHistory(), bufferIO);
    }

    private InputPrompter confirming(String value) {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private InputPrompter cancelling() {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
    }

    /**
     * 呼び出し順に異なる値を返すプロンプター。
     * 1回目のプロンプトにはfirstValue、2回目にはsecondValueを返す。
     */
    private InputPrompter sequentialPrompter(String firstValue, String secondValue) {
        var callCount = new AtomicInteger(0);
        return (message, history) -> {
            var value = callCount.getAndIncrement() == 0 ? firstValue : secondValue;
            return CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
        };
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
            var barBuffer = new BufferFacade(new TextBuffer("bar.txt", new GapTextModel(), new SettingsRegistry()));
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
            var barBuffer = new BufferFacade(new TextBuffer("bar.txt", new GapTextModel(), new SettingsRegistry()));
            bufferManager.add(barBuffer);
            // window1: scratch, window2: fooBuffer
            frame.splitActiveWindow(Direction.VERTICAL, fooBuffer);
            // scratchを削除 → barが他ウィンドウで表示されていないので優先
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*scratch*"));

            cmd.execute(context).join();

            // scratch削除後に再作成されるので、barかscratchが切り替え先
            var windows = frame.getWindowTree().windows();
            var switchedWindow = windows.detect(w -> !w.getBuffer().equals(fooBuffer));
            // barが他ウィンドウで表示されていないので優先される
            assertEquals(barBuffer, switchedWindow.getBuffer());
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

            assertEquals(fooBuffer, frame.getActiveWindow().getBuffer());
        }
    }

    @Nested
    class システムバッファ {

        @Test
        void システムバッファは削除できない() {
            var msgBuffer = new BufferFacade(new MessageBuffer("*Messages*", 100, new SettingsRegistry()));
            bufferManager.add(msgBuffer);
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*Messages*"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("*Messages*").isPresent());
            assertEquals(3, bufferManager.size());
        }

        @Test
        void システムバッファの削除拒否時にエコーエリアにメッセージが表示される() {
            var msgBuffer = new BufferFacade(new MessageBuffer("*Messages*", 100, new SettingsRegistry()));
            bufferManager.add(msgBuffer);
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*Messages*"));

            cmd.execute(context).join();

            assertTrue(context.messageBuffer().isShowingMessage());
        }
    }

    @Nested
    class dirtyバッファの確認 {

        @Test
        void dirtyバッファでyesを選ぶと保存せず削除される() {
            fooBuffer.setFilePath(Path.of("/tmp/foo.txt"));
            fooBuffer.insertText(0, "unsaved");
            fooBuffer.markDirty();

            var context = TestCommandContextFactory.create(frame, bufferManager, sequentialPrompter("foo.txt", "yes"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("foo.txt").isEmpty());
            assertTrue(writerStorage.isEmpty());
        }

        @Test
        void dirtyバッファでnoを選ぶと削除されない() {
            fooBuffer.insertText(0, "unsaved");
            fooBuffer.markDirty();

            var context = TestCommandContextFactory.create(frame, bufferManager, sequentialPrompter("foo.txt", "no"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("foo.txt").isPresent());
        }

        @Test
        void dirtyバッファでsave_and_killを選ぶと保存してから削除される() {
            fooBuffer.setFilePath(Path.of("/tmp/foo.txt"));
            fooBuffer.insertText(0, "content to save");
            fooBuffer.markDirty();

            var context = TestCommandContextFactory.create(
                    frame, bufferManager, sequentialPrompter("foo.txt", "save and kill"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("foo.txt").isEmpty());
            assertEquals(
                    "content to save",
                    Objects.requireNonNull(writerStorage.get("/tmp/foo.txt")).toString());
        }

        @Test
        void dirtyバッファでsave_and_killを選んだがファイルパス未設定の場合はメッセージ表示して中止() {
            fooBuffer.insertText(0, "unsaved");
            fooBuffer.markDirty();

            var context = TestCommandContextFactory.create(
                    frame, bufferManager, sequentialPrompter("foo.txt", "save and kill"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("foo.txt").isPresent());
            assertEquals(
                    "Buffer has no file path; use save-buffer first: foo.txt",
                    context.messageBuffer().getLastMessage().orElseThrow());
        }

        @Test
        void dirtyでないバッファは確認なしで削除される() {
            assertFalse(fooBuffer.isDirty());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("foo.txt"));

            cmd.execute(context).join();

            assertTrue(bufferManager.findByName("foo.txt").isEmpty());
        }
    }

    @Nested
    class previousBufferのクリーンアップ {

        @Test
        void 削除対象がpreviousBufferの場合にクリアされる() {
            // scratch → foo に切り替え → scratch が previousBuffer になる
            frame.getActiveWindow().setBuffer(fooBuffer);
            assertEquals(scratch, frame.getActiveWindow().getPreviousBuffer().orElseThrow());

            // scratch を削除
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("*scratch*"));
            cmd.execute(context).join();

            // previousBuffer に元の scratch が残っていないこと
            // (setBufferで切り替えが発生するのでpreviousBufferは更新される)
            var prevOpt = frame.getActiveWindow().getPreviousBuffer();
            if (prevOpt.isPresent()) {
                assertNotEquals(scratch, prevOpt.get());
            }
        }
    }
}
