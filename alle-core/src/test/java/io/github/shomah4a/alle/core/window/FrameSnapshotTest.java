package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FrameSnapshotTest {

    private final SettingsRegistry settings = new SettingsRegistry();

    private BufferFacade createBuffer(String name) {
        return new BufferFacade(new TextBuffer(name, new GapTextModel(), settings));
    }

    private BufferFacade createBuffer(String name, String content) {
        var model = new GapTextModel();
        model.insert(0, content);
        return new BufferFacade(new TextBuffer(name, model, settings));
    }

    private Frame createFrame(BufferFacade buffer) {
        var window = new Window(buffer);
        var minibuffer = new Window(createBuffer("*Minibuffer*"));
        return new Frame(window, minibuffer);
    }

    @Nested
    class captureSnapshot {

        @Test
        void 単一ウィンドウのスナップショットをキャプチャする() {
            var buffer = createBuffer("test.txt", "hello");
            var frame = createFrame(buffer);
            frame.getActiveWindow().setPoint(3);

            var snapshot = frame.captureSnapshot();

            assertInstanceOf(WindowTreeSnapshot.Leaf.class, snapshot.tree());
            var leaf = (WindowTreeSnapshot.Leaf) snapshot.tree();
            assertEquals(
                    new BufferIdentifier.ByName("test.txt"),
                    leaf.snapshot().current().identifier());
            assertEquals(3, leaf.snapshot().current().viewState().point());
            assertEquals(0, snapshot.activeWindowIndex());
        }

        @Test
        void 分割ウィンドウのスナップショットをキャプチャする() {
            var buffer1 = createBuffer("a.txt");
            var buffer2 = createBuffer("b.txt");
            var frame = createFrame(buffer1);
            frame.splitActiveWindow(Direction.VERTICAL, buffer2);

            var snapshot = frame.captureSnapshot();

            assertInstanceOf(WindowTreeSnapshot.Split.class, snapshot.tree());
            var split = (WindowTreeSnapshot.Split) snapshot.tree();
            assertEquals(Direction.VERTICAL, split.direction());
            assertEquals(0.5, split.ratio());
            assertEquals(1, snapshot.activeWindowIndex());
        }

        @Test
        void アクティブウィンドウのインデックスを正しく記録する() {
            var buf1 = createBuffer("first");
            var buf2 = createBuffer("second");
            var buf3 = createBuffer("third");
            var frame = createFrame(buf1);
            frame.splitActiveWindow(Direction.VERTICAL, buf2);
            frame.splitActiveWindow(Direction.HORIZONTAL, buf3);

            var snapshot = frame.captureSnapshot();

            assertEquals(2, snapshot.activeWindowIndex());
        }
    }

    @Nested
    class restoreSnapshot {

        @Test
        void 単一ウィンドウを復元する() {
            var buffer = createBuffer("test.txt", "hello");
            var bufferManager = new BufferManager();
            bufferManager.add(buffer);
            var scratch = createBuffer("*scratch*");
            bufferManager.add(scratch);

            var frame = createFrame(buffer);
            frame.getActiveWindow().setPoint(3);
            var snapshot = frame.captureSnapshot();

            frame.getActiveWindow().setPoint(0);
            frame.restoreSnapshot(snapshot, bufferManager, scratch);

            assertEquals("test.txt", frame.getActiveWindow().getBuffer().getName());
            assertEquals(3, frame.getActiveWindow().getPoint());
        }

        @Test
        void 分割構造を復元する() {
            var buf1 = createBuffer("a.txt");
            var buf2 = createBuffer("b.txt");
            var bufferManager = new BufferManager();
            bufferManager.add(buf1);
            bufferManager.add(buf2);
            var scratch = createBuffer("*scratch*");
            bufferManager.add(scratch);

            var frame = createFrame(buf1);
            frame.splitActiveWindow(Direction.VERTICAL, buf2);
            var snapshot = frame.captureSnapshot();

            frame.deleteOtherWindows();
            frame.restoreSnapshot(snapshot, bufferManager, scratch);

            var tree = frame.getWindowTree();
            assertInstanceOf(WindowTree.Split.class, tree);
            var split = (WindowTree.Split) tree;
            assertEquals(Direction.VERTICAL, split.direction());
            var windows = tree.windows();
            assertEquals(2, windows.size());
            assertEquals("a.txt", windows.get(0).getBuffer().getName());
            assertEquals("b.txt", windows.get(1).getBuffer().getName());
        }

        @Test
        void アクティブウィンドウインデックスを復元する() {
            var buf1 = createBuffer("first");
            var buf2 = createBuffer("second");
            var bufferManager = new BufferManager();
            bufferManager.add(buf1);
            bufferManager.add(buf2);
            var scratch = createBuffer("*scratch*");
            bufferManager.add(scratch);

            var frame = createFrame(buf1);
            frame.splitActiveWindow(Direction.VERTICAL, buf2);
            var snapshot = frame.captureSnapshot();

            frame.deleteOtherWindows();
            frame.restoreSnapshot(snapshot, bufferManager, scratch);

            assertEquals("second", frame.getActiveWindow().getBuffer().getName());
        }

        @Test
        void バッファが存在しない場合historyから生存バッファを探索する() {
            var buf1 = createBuffer("alive.txt", "content");
            var buf2 = createBuffer("deleted.txt");
            var bufferManager = new BufferManager();
            bufferManager.add(buf1);
            bufferManager.add(buf2);
            var scratch = createBuffer("*scratch*");
            bufferManager.add(scratch);

            var frame = createFrame(buf2);
            frame.getActiveWindow().setBuffer(buf1);
            frame.getActiveWindow().setBuffer(buf2);
            var snapshot = frame.captureSnapshot();

            bufferManager.remove("deleted.txt");
            frame.restoreSnapshot(snapshot, bufferManager, scratch);

            assertEquals("alive.txt", frame.getActiveWindow().getBuffer().getName());
        }

        @Test
        void 全バッファ不在の場合fallbackバッファを使用する() {
            var buf = createBuffer("temp.txt");
            var bufferManager = new BufferManager();
            bufferManager.add(buf);
            var scratch = createBuffer("*scratch*");
            bufferManager.add(scratch);

            var frame = createFrame(buf);
            var snapshot = frame.captureSnapshot();

            bufferManager.remove("temp.txt");
            frame.restoreSnapshot(snapshot, bufferManager, scratch);

            assertEquals("*scratch*", frame.getActiveWindow().getBuffer().getName());
        }

        @Test
        void truncateLinesを復元する() {
            var buffer = createBuffer("test.txt");
            var bufferManager = new BufferManager();
            bufferManager.add(buffer);
            var scratch = createBuffer("*scratch*");
            bufferManager.add(scratch);

            var frame = createFrame(buffer);
            frame.getActiveWindow().setTruncateLines(true);
            var snapshot = frame.captureSnapshot();

            frame.restoreSnapshot(snapshot, bufferManager, scratch);

            assertEquals(true, frame.getActiveWindow().isTruncateLines());
        }

        @Test
        void 復元後のウィンドウは元のウィンドウとは異なるインスタンスになる() {
            var buffer = createBuffer("test.txt");
            var bufferManager = new BufferManager();
            bufferManager.add(buffer);
            var scratch = createBuffer("*scratch*");
            bufferManager.add(scratch);

            var frame = createFrame(buffer);
            var originalWindow = frame.getActiveWindow();
            var snapshot = frame.captureSnapshot();

            frame.restoreSnapshot(snapshot, bufferManager, scratch);

            assertNotSame(originalWindow, frame.getActiveWindow());
        }
    }
}
