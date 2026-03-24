package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FrameTest {

    private Buffer createBuffer(String name) {
        return new EditableBuffer(name, new GapTextModel());
    }

    private Frame createFrame() {
        var initialWindow = new Window(createBuffer("main"));
        var minibufferWindow = new Window(createBuffer("*Minibuffer*"));
        return new Frame(initialWindow, minibufferWindow);
    }

    @Nested
    class 初期状態 {

        @Test
        void 初期ウィンドウがアクティブで単一Leafのツリーを持つ() {
            var frame = createFrame();
            assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
            assertSame(frame.getActiveWindow(), ((WindowTree.Leaf) frame.getWindowTree()).window());
        }

        @Test
        void ミニバッファウィンドウを保持する() {
            var frame = createFrame();
            assertEquals("*Minibuffer*", frame.getMinibufferWindow().getBuffer().getName());
        }
    }

    @Nested
    class ウィンドウ分割 {

        @Test
        void アクティブウィンドウを垂直分割できる() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            var newBuffer = createBuffer("new");

            var newWindow = frame.splitActiveWindow(Direction.VERTICAL, newBuffer);

            var split = assertInstanceOf(WindowTree.Split.class, frame.getWindowTree());
            assertEquals(Direction.VERTICAL, split.direction());
            assertSame(originalWindow, ((WindowTree.Leaf) split.first()).window());
            assertSame(newWindow, ((WindowTree.Leaf) split.second()).window());
            assertSame(newWindow, frame.getActiveWindow());
        }

        @Test
        void アクティブウィンドウを水平分割できる() {
            var frame = createFrame();
            var newBuffer = createBuffer("new");

            frame.splitActiveWindow(Direction.HORIZONTAL, newBuffer);

            var split = assertInstanceOf(WindowTree.Split.class, frame.getWindowTree());
            assertEquals(Direction.HORIZONTAL, split.direction());
        }

        @Test
        void 分割後に更に分割できる() {
            var frame = createFrame();
            frame.splitActiveWindow(Direction.VERTICAL, createBuffer("b"));
            frame.splitActiveWindow(Direction.HORIZONTAL, createBuffer("c"));

            var root = assertInstanceOf(WindowTree.Split.class, frame.getWindowTree());
            assertInstanceOf(WindowTree.Leaf.class, root.first());
            var secondSplit = assertInstanceOf(WindowTree.Split.class, root.second());
            assertInstanceOf(WindowTree.Leaf.class, secondSplit.first());
            assertInstanceOf(WindowTree.Leaf.class, secondSplit.second());
        }

        @Test
        void 同じバッファで分割して独立したポイントを持てる() {
            var frame = createFrame();
            var buffer = frame.getActiveWindow().getBuffer();
            frame.getActiveWindow().insert("Hello World");
            frame.getActiveWindow().setPoint(5);

            var newWindow = frame.splitActiveWindow(Direction.VERTICAL, buffer);

            assertEquals(0, newWindow.getPoint());
            // 元のウィンドウのポイントは変わらない
            var originalWindow = ((WindowTree.Leaf) ((WindowTree.Split) frame.getWindowTree()).first()).window();
            assertEquals(5, originalWindow.getPoint());
        }
    }

    @Nested
    class ウィンドウ削除 {

        @Test
        void 最後のウィンドウは削除できない() {
            var frame = createFrame();
            var result = frame.deleteWindow(frame.getActiveWindow());
            assertFalse(result);
            assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
        }

        @Test
        void 分割後にウィンドウを削除できる() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            frame.splitActiveWindow(Direction.VERTICAL, createBuffer("b"));

            var result = frame.deleteWindow(frame.getActiveWindow());

            assertTrue(result);
            var leaf = assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
            assertSame(originalWindow, leaf.window());
        }

        @Test
        void アクティブウィンドウを削除すると別のウィンドウがアクティブになる() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            var newWindow = frame.splitActiveWindow(Direction.VERTICAL, createBuffer("b"));
            assertSame(newWindow, frame.getActiveWindow());

            frame.deleteWindow(newWindow);

            assertSame(originalWindow, frame.getActiveWindow());
        }

        @Test
        void 非アクティブウィンドウを削除してもアクティブウィンドウは変わらない() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            var newWindow = frame.splitActiveWindow(Direction.VERTICAL, createBuffer("b"));
            // newWindowがアクティブ

            frame.deleteWindow(originalWindow);

            assertSame(newWindow, frame.getActiveWindow());
        }

        @Test
        void 存在しないウィンドウの削除は失敗する() {
            var frame = createFrame();
            var otherWindow = new Window(createBuffer("other"));
            assertFalse(frame.deleteWindow(otherWindow));
        }
    }

    @Nested
    class 他ウィンドウ全削除 {

        @Test
        void 複数ウィンドウからアクティブウィンドウのみが残る() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            frame.splitActiveWindow(Direction.VERTICAL, createBuffer("b"));
            frame.setActiveWindow(originalWindow);
            frame.splitActiveWindow(Direction.HORIZONTAL, createBuffer("c"));
            frame.setActiveWindow(originalWindow);
            assertEquals(3, frame.getWindowTree().windows().size());

            frame.deleteOtherWindows();

            var leaf = assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
            assertSame(originalWindow, leaf.window());
            assertSame(originalWindow, frame.getActiveWindow());
        }

        @Test
        void ウィンドウが1つの場合でもエラーにならない() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();

            frame.deleteOtherWindows();

            var leaf = assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
            assertSame(originalWindow, leaf.window());
        }
    }

    @Nested
    class アクティブウィンドウ設定 {

        @Test
        void ツリーに含まれるウィンドウをアクティブにできる() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            frame.splitActiveWindow(Direction.VERTICAL, createBuffer("b"));

            frame.setActiveWindow(originalWindow);

            assertSame(originalWindow, frame.getActiveWindow());
        }

        @Test
        void ツリーに含まれないウィンドウを設定すると例外が発生する() {
            var frame = createFrame();
            var otherWindow = new Window(createBuffer("other"));

            assertThrows(IllegalArgumentException.class, () -> frame.setActiveWindow(otherWindow));
        }

        @Test
        void ミニバッファウィンドウはアクティブに設定できない() {
            var frame = createFrame();

            assertThrows(IllegalArgumentException.class, () -> frame.setActiveWindow(frame.getMinibufferWindow()));
        }
    }

    @Nested
    class スナップショット生成 {

        private MessageBuffer createMessageBuffer() {
            return new MessageBuffer("*Messages*", 100);
        }

        @Test
        void 空バッファで画面サイズ分のスナップショットが生成される() {
            var frame = createFrame();
            var msgBuf = createMessageBuffer();

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            assertEquals(80, snapshot.screenCols());
            assertEquals(24, snapshot.screenRows());
            assertEquals(1, snapshot.windowSnapshots().size());
        }

        @Test
        void ウィンドウスナップショットにモードライン文字列が含まれる() {
            var frame = createFrame();
            var msgBuf = createMessageBuffer();

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            var ws = snapshot.windowSnapshots().get(0);
            assertTrue(ws.modeLine().contains("main"));
        }

        @Test
        void バッファにテキストがある場合に可視行がスナップショットに含まれる() {
            var buffer = new EditableBuffer("test", new GapTextModel());
            buffer.insertText(0, "line1\nline2\nline3");
            var window = new Window(buffer);
            var minibufferWindow = new Window(createBuffer("*Minibuffer*"));
            var frame = new Frame(window, minibufferWindow);
            var msgBuf = createMessageBuffer();

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            var ws = snapshot.windowSnapshots().get(0);
            assertEquals(3, ws.visibleLines().size());
            assertEquals("line1", ws.visibleLines().get(0).text());
            assertEquals("line2", ws.visibleLines().get(1).text());
            assertEquals("line3", ws.visibleLines().get(2).text());
        }

        @Test
        void 分割されたウィンドウが複数のスナップショットとして含まれる() {
            var frame = createFrame();
            frame.splitActiveWindow(Direction.HORIZONTAL, createBuffer("second"));
            var msgBuf = createMessageBuffer();

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            assertEquals(2, snapshot.windowSnapshots().size());
        }

        @Test
        void メッセージ表示中はミニバッファにメッセージテキストが含まれる() {
            var frame = createFrame();
            var msgBuf = createMessageBuffer();
            msgBuf.message("Hello");

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            assertTrue(snapshot.minibuffer().text().isPresent());
            assertEquals("Hello", snapshot.minibuffer().text().get());
        }

        @Test
        void メッセージ未表示時はミニバッファが空になる() {
            var frame = createFrame();
            var msgBuf = createMessageBuffer();

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            assertTrue(snapshot.minibuffer().text().isEmpty());
        }

        @Test
        void ミニバッファアクティブ時はミニバッファのテキストがスナップショットに含まれる() {
            var frame = createFrame();
            var minibuffer = frame.getMinibufferWindow();
            minibuffer.getBuffer().insertText(0, "Find file: ");
            frame.activateMinibuffer();
            var msgBuf = createMessageBuffer();

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            assertTrue(snapshot.minibuffer().text().isPresent());
            assertEquals("Find file: ", snapshot.minibuffer().text().get());
        }

        @Test
        void カーソル位置がアクティブウィンドウのポイントに対応する() {
            var buffer = new EditableBuffer("test", new GapTextModel());
            buffer.insertText(0, "Hello");
            var window = new Window(buffer);
            window.setPoint(3);
            var minibufferWindow = new Window(createBuffer("*Minibuffer*"));
            var frame = new Frame(window, minibufferWindow);
            var msgBuf = createMessageBuffer();

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            // point=3 なのでカラム3にカーソルがある
            assertEquals(3, snapshot.cursorColumn());
            assertEquals(0, snapshot.cursorRow());
        }

        @Test
        void カーソルが二行目にある場合のカーソル行が正しい() {
            var buffer = new EditableBuffer("test", new GapTextModel());
            buffer.insertText(0, "line1\nline2");
            var window = new Window(buffer);
            window.setPoint(8); // "line1\nli" の位置 = 2行目のカラム2
            var minibufferWindow = new Window(createBuffer("*Minibuffer*"));
            var frame = new Frame(window, minibufferWindow);
            var msgBuf = createMessageBuffer();

            var snapshot = frame.createSnapshot(80, 24, msgBuf);

            assertEquals(2, snapshot.cursorColumn());
            assertEquals(1, snapshot.cursorRow());
        }

        @Test
        void 画面行数が3未満でもウィンドウ高さ不足のスナップショットが生成される() {
            var frame = createFrame();
            var msgBuf = createMessageBuffer();

            // rows=2 → windowAreaRows=1, モードラインで1行使うので bufferRows=0
            var snapshot = frame.createSnapshot(80, 2, msgBuf);

            assertEquals(2, snapshot.screenRows());
            // height < 2 のウィンドウはスキップされる
            assertEquals(0, snapshot.windowSnapshots().size());
        }
    }
}
