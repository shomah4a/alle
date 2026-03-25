package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WindowLayoutTest {

    private Window createWindow() {
        return new Window(new BufferFacade(new EditableBuffer("test", new GapTextModel())));
    }

    @Nested
    class 単一ウィンドウ {

        @Test
        void Leafの場合は利用可能領域全体が割り当てられる() {
            var window = createWindow();
            var tree = new WindowTree.Leaf(window);
            var available = new Rect(0, 0, 80, 24);

            var result = WindowLayout.compute(tree, available);

            assertEquals(1, result.windowRects().size());
            var rect = result.windowRects().get(window);
            assertEquals(0, rect.top());
            assertEquals(0, rect.left());
            assertEquals(80, rect.width());
            assertEquals(24, rect.height());
        }
    }

    @Nested
    class 水平分割 {

        @Test
        void 上下均等に分割される() {
            var window1 = createWindow();
            var window2 = createWindow();
            var tree = new WindowTree.Split(
                    Direction.HORIZONTAL, 0.5, new WindowTree.Leaf(window1), new WindowTree.Leaf(window2));
            var available = new Rect(0, 0, 80, 24);

            var result = WindowLayout.compute(tree, available);

            assertEquals(2, result.windowRects().size());
            var rect1 = result.windowRects().get(window1);
            assertEquals(0, rect1.top());
            assertEquals(80, rect1.width());
            assertEquals(12, rect1.height());

            var rect2 = result.windowRects().get(window2);
            assertEquals(12, rect2.top());
            assertEquals(80, rect2.width());
            assertEquals(12, rect2.height());
        }

        @Test
        void 奇数行数の場合はfirst側が1行少なくなる() {
            var window1 = createWindow();
            var window2 = createWindow();
            var tree = new WindowTree.Split(
                    Direction.HORIZONTAL, 0.5, new WindowTree.Leaf(window1), new WindowTree.Leaf(window2));
            var available = new Rect(0, 0, 80, 25);

            var result = WindowLayout.compute(tree, available);

            var rect1 = result.windowRects().get(window1);
            var rect2 = result.windowRects().get(window2);
            // 12 + 13 = 25
            assertEquals(12, rect1.height());
            assertEquals(13, rect2.height());
            assertEquals(rect1.height() + rect2.height(), available.height());
        }
    }

    @Nested
    class 垂直分割 {

        @Test
        void 左右均等に分割されセパレータ1カラムが確保される() {
            var window1 = createWindow();
            var window2 = createWindow();
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(window1), new WindowTree.Leaf(window2));
            var available = new Rect(0, 0, 81, 24);

            var result = WindowLayout.compute(tree, available);

            assertEquals(2, result.windowRects().size());
            var rect1 = result.windowRects().get(window1);
            assertEquals(0, rect1.left());
            assertEquals(40, rect1.width());
            assertEquals(24, rect1.height());

            var rect2 = result.windowRects().get(window2);
            assertEquals(41, rect2.left()); // 40 + 1(separator)
            assertEquals(40, rect2.width());
            assertEquals(24, rect2.height());

            // セパレータ位置の検証
            assertEquals(1, result.separators().size());
            var sep = result.separators().get(0);
            assertEquals(40, sep.column());
            assertEquals(0, sep.top());
            assertEquals(24, sep.height());
        }

        @Test
        void セパレータ分を含めた合計幅が利用可能幅に収まる() {
            var window1 = createWindow();
            var window2 = createWindow();
            var tree = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(window1), new WindowTree.Leaf(window2));
            var available = new Rect(0, 0, 80, 24);

            var result = WindowLayout.compute(tree, available);

            var rect1 = result.windowRects().get(window1);
            var rect2 = result.windowRects().get(window2);
            // first.width + separator(1) + second.width <= available.width
            int totalWidth = rect1.width() + 1 + rect2.width();
            assertEquals(available.width(), totalWidth);
        }
    }

    @Nested
    class ネスト分割 {

        @Test
        void 水平分割の中に垂直分割がネストされる() {
            var window1 = createWindow();
            var window2 = createWindow();
            var window3 = createWindow();
            // window1 上 / (window2 左 | window3 右)
            var bottomSplit = new WindowTree.Split(
                    Direction.VERTICAL, 0.5, new WindowTree.Leaf(window2), new WindowTree.Leaf(window3));
            var tree = new WindowTree.Split(Direction.HORIZONTAL, 0.5, new WindowTree.Leaf(window1), bottomSplit);
            var available = new Rect(0, 0, 81, 24);

            var result = WindowLayout.compute(tree, available);

            assertEquals(3, result.windowRects().size());

            var rect1 = result.windowRects().get(window1);
            assertEquals(0, rect1.top());
            assertEquals(0, rect1.left());
            assertEquals(81, rect1.width());
            assertEquals(12, rect1.height());

            var rect2 = result.windowRects().get(window2);
            assertEquals(12, rect2.top());
            assertEquals(0, rect2.left());
            assertEquals(40, rect2.width());

            var rect3 = result.windowRects().get(window3);
            assertEquals(12, rect3.top());
            assertEquals(41, rect3.left());
            assertEquals(40, rect3.width());
        }
    }

    @Nested
    class オフセット付き領域 {

        @Test
        void 非ゼロのtop_leftからでも正しく計算される() {
            var window1 = createWindow();
            var window2 = createWindow();
            var tree = new WindowTree.Split(
                    Direction.HORIZONTAL, 0.5, new WindowTree.Leaf(window1), new WindowTree.Leaf(window2));
            var available = new Rect(5, 10, 60, 20);

            var result = WindowLayout.compute(tree, available);

            var rect1 = result.windowRects().get(window1);
            assertEquals(5, rect1.top());
            assertEquals(10, rect1.left());
            assertEquals(60, rect1.width());
            assertEquals(10, rect1.height());

            var rect2 = result.windowRects().get(window2);
            assertEquals(15, rect2.top());
            assertEquals(10, rect2.left());
            assertEquals(60, rect2.width());
            assertEquals(10, rect2.height());
        }
    }
}
