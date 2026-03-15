package io.github.shomah4a.alle.core.window;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

/**
 * WindowTreeから各ウィンドウへの矩形割り当てを計算する。
 * 各ウィンドウの矩形にはバッファ表示エリアとモードラインの両方を含む。
 * モードラインは矩形の最終行に配置される。
 */
public class WindowLayout {

    /** 垂直分割時のウィンドウ間セパレータ幅（カラム数） */
    private static final int SEPARATOR_WIDTH = 1;

    private WindowLayout() {}

    /**
     * WindowTreeと利用可能領域から、各ウィンドウの矩形とセパレータ位置を計算する。
     *
     * @param tree      ウィンドウツリー
     * @param available 利用可能な矩形領域
     * @return レイアウト結果（ウィンドウ矩形 + セパレータ位置）
     */
    public static LayoutResult compute(WindowTree tree, Rect available) {
        MutableMap<Window, Rect> windowRects = Maps.mutable.empty();
        MutableList<Separator> separators = Lists.mutable.empty();
        computeRecursive(tree, available, windowRects, separators);
        return new LayoutResult(windowRects.toImmutable(), separators);
    }

    private static void computeRecursive(
            WindowTree tree, Rect available, MutableMap<Window, Rect> windowRects, MutableList<Separator> separators) {
        switch (tree) {
            case WindowTree.Leaf leaf -> windowRects.put(leaf.window(), available);
            case WindowTree.Split split -> {
                if (split.direction() == Direction.HORIZONTAL) {
                    layoutHorizontal(split, available, windowRects, separators);
                } else {
                    layoutVertical(split, available, windowRects, separators);
                }
            }
        }
    }

    private static void layoutHorizontal(
            WindowTree.Split split,
            Rect available,
            MutableMap<Window, Rect> windowRects,
            MutableList<Separator> separators) {
        int firstHeight = (int) (available.height() * split.ratio());
        firstHeight = Math.max(firstHeight, 1);
        int secondHeight = available.height() - firstHeight;
        secondHeight = Math.max(secondHeight, 1);
        if (firstHeight + secondHeight > available.height()) {
            firstHeight = available.height() - secondHeight;
        }

        var firstRect = new Rect(available.top(), available.left(), available.width(), firstHeight);
        var secondRect = new Rect(available.top() + firstHeight, available.left(), available.width(), secondHeight);

        computeRecursive(split.first(), firstRect, windowRects, separators);
        computeRecursive(split.second(), secondRect, windowRects, separators);
    }

    private static void layoutVertical(
            WindowTree.Split split,
            Rect available,
            MutableMap<Window, Rect> windowRects,
            MutableList<Separator> separators) {
        int firstWidth = (int) (available.width() * split.ratio());
        firstWidth = Math.max(firstWidth, 1);
        int secondWidth = available.width() - firstWidth - SEPARATOR_WIDTH;
        secondWidth = Math.max(secondWidth, 1);
        if (firstWidth + SEPARATOR_WIDTH + secondWidth > available.width()) {
            firstWidth = available.width() - SEPARATOR_WIDTH - secondWidth;
        }

        int separatorCol = available.left() + firstWidth;
        separators.add(new Separator(separatorCol, available.top(), available.height()));

        var firstRect = new Rect(available.top(), available.left(), firstWidth, available.height());
        var secondRect = new Rect(
                available.top(), available.left() + firstWidth + SEPARATOR_WIDTH, secondWidth, available.height());

        computeRecursive(split.first(), firstRect, windowRects, separators);
        computeRecursive(split.second(), secondRect, windowRects, separators);
    }
}
