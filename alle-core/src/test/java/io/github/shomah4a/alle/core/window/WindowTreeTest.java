package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WindowTreeTest {

    private Window createWindow(String name) {
        return new Window(new EditableBuffer(name, new GapTextModel()));
    }

    @Nested
    class split {

        @Test
        void 単一ウィンドウを垂直分割できる() {
            var window = createWindow("a");
            var newWindow = createWindow("b");
            var tree = new WindowTree.Leaf(window);

            var result = tree.split(window, Direction.VERTICAL, newWindow);

            assertTrue(result.isPresent());
            var split = assertInstanceOf(WindowTree.Split.class, result.get());
            assertEquals(Direction.VERTICAL, split.direction());
            assertEquals(0.5, split.ratio());
            assertSame(window, ((WindowTree.Leaf) split.first()).window());
            assertSame(newWindow, ((WindowTree.Leaf) split.second()).window());
        }

        @Test
        void 単一ウィンドウを水平分割できる() {
            var window = createWindow("a");
            var newWindow = createWindow("b");
            var tree = new WindowTree.Leaf(window);

            var result = tree.split(window, Direction.HORIZONTAL, newWindow);

            assertTrue(result.isPresent());
            var split = assertInstanceOf(WindowTree.Split.class, result.get());
            assertEquals(Direction.HORIZONTAL, split.direction());
        }

        @Test
        void 分割済みツリーのfirst側を更に分割できる() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var windowC = createWindow("c");
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(windowA), new WindowTree.Leaf(windowB));

            var result = tree.split(windowA, Direction.HORIZONTAL, windowC);

            assertTrue(result.isPresent());
            var root = assertInstanceOf(WindowTree.Split.class, result.get());
            var firstSplit = assertInstanceOf(WindowTree.Split.class, root.first());
            assertEquals(Direction.HORIZONTAL, firstSplit.direction());
            assertSame(windowA, ((WindowTree.Leaf) firstSplit.first()).window());
            assertSame(windowC, ((WindowTree.Leaf) firstSplit.second()).window());
            assertSame(windowB, ((WindowTree.Leaf) root.second()).window());
        }

        @Test
        void 分割済みツリーのsecond側を更に分割できる() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var windowC = createWindow("c");
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(windowA), new WindowTree.Leaf(windowB));

            var result = tree.split(windowB, Direction.VERTICAL, windowC);

            assertTrue(result.isPresent());
            var root = assertInstanceOf(WindowTree.Split.class, result.get());
            assertSame(windowA, ((WindowTree.Leaf) root.first()).window());
            var secondSplit = assertInstanceOf(WindowTree.Split.class, root.second());
            assertSame(windowB, ((WindowTree.Leaf) secondSplit.first()).window());
            assertSame(windowC, ((WindowTree.Leaf) secondSplit.second()).window());
        }

        @Test
        void 存在しないウィンドウの分割はemptyを返す() {
            var window = createWindow("a");
            var other = createWindow("other");
            var tree = new WindowTree.Leaf(window);

            var result = tree.split(other, Direction.VERTICAL, createWindow("b"));

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class remove {

        @Test
        void 最後のウィンドウは削除できない() {
            var window = createWindow("a");
            var tree = new WindowTree.Leaf(window);

            var result = tree.remove(window);

            assertTrue(result.isEmpty());
        }

        @Test
        void 二分割のfirst側を削除するとsecond側が残る() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(windowA), new WindowTree.Leaf(windowB));

            var result = tree.remove(windowA);

            assertTrue(result.isPresent());
            var leaf = assertInstanceOf(WindowTree.Leaf.class, result.get());
            assertSame(windowB, leaf.window());
        }

        @Test
        void 二分割のsecond側を削除するとfirst側が残る() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(windowA), new WindowTree.Leaf(windowB));

            var result = tree.remove(windowB);

            assertTrue(result.isPresent());
            var leaf = assertInstanceOf(WindowTree.Leaf.class, result.get());
            assertSame(windowA, leaf.window());
        }

        @Test
        void 三つ以上のウィンドウから中間を削除してもツリー構造が維持される() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var windowC = createWindow("c");
            // A | (B / C) の構造
            var tree = new WindowTree.Split(
                    Direction.VERTICAL,
                    0.5,
                    new WindowTree.Leaf(windowA),
                    new WindowTree.Split(
                            Direction.HORIZONTAL, 0.5, new WindowTree.Leaf(windowB), new WindowTree.Leaf(windowC)));

            var result = tree.remove(windowB);

            assertTrue(result.isPresent());
            var root = assertInstanceOf(WindowTree.Split.class, result.get());
            assertSame(windowA, ((WindowTree.Leaf) root.first()).window());
            var remainSecond = assertInstanceOf(WindowTree.Leaf.class, root.second());
            assertSame(windowC, remainSecond.window());
        }

        @Test
        void 存在しないウィンドウの削除はemptyを返す() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var other = createWindow("other");
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(windowA), new WindowTree.Leaf(windowB));

            var result = tree.remove(other);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class windows {

        @Test
        void 単一ウィンドウのツリーは1要素のリストを返す() {
            var window = createWindow("a");
            var tree = new WindowTree.Leaf(window);

            var result = tree.windows();

            assertEquals(1, result.size());
            assertSame(window, result.get(0));
        }

        @Test
        void 二分割のツリーは深さ優先順で2要素のリストを返す() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(windowA), new WindowTree.Leaf(windowB));

            var result = tree.windows();

            assertEquals(2, result.size());
            assertSame(windowA, result.get(0));
            assertSame(windowB, result.get(1));
        }

        @Test
        void ネストした分割でも深さ優先順で全ウィンドウを返す() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var windowC = createWindow("c");
            // A | (B / C) の構造
            var tree = new WindowTree.Split(
                    Direction.VERTICAL,
                    0.5,
                    new WindowTree.Leaf(windowA),
                    new WindowTree.Split(
                            Direction.HORIZONTAL, 0.5, new WindowTree.Leaf(windowB), new WindowTree.Leaf(windowC)));

            var result = tree.windows();

            assertEquals(3, result.size());
            assertSame(windowA, result.get(0));
            assertSame(windowB, result.get(1));
            assertSame(windowC, result.get(2));
        }
    }

    @Nested
    class contains {

        @Test
        void Leafに含まれるウィンドウを検出できる() {
            var window = createWindow("a");
            var tree = new WindowTree.Leaf(window);
            assertTrue(tree.contains(window));
        }

        @Test
        void Splitの子に含まれるウィンドウを検出できる() {
            var windowA = createWindow("a");
            var windowB = createWindow("b");
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(windowA), new WindowTree.Leaf(windowB));
            assertTrue(tree.contains(windowA));
            assertTrue(tree.contains(windowB));
        }

        @Test
        void 含まれないウィンドウはfalseを返す() {
            var window = createWindow("a");
            var other = createWindow("other");
            var tree = new WindowTree.Leaf(window);
            assertFalse(tree.contains(other));
        }
    }
}
