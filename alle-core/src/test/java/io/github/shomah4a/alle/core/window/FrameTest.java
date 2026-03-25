package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
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
}
