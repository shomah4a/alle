package io.github.shomah4a.alle.core.window;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
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
     * WindowTreeと利用可能領域から、各ウィンドウの矩形を計算する。
     *
     * @param tree      ウィンドウツリー
     * @param available 利用可能な矩形領域
     * @return ウィンドウから矩形へのマッピング
     */
    public static ImmutableMap<Window, Rect> compute(WindowTree tree, Rect available) {
        MutableMap<Window, Rect> result = Maps.mutable.empty();
        computeRecursive(tree, available, result);
        return result.toImmutable();
    }

    private static void computeRecursive(WindowTree tree, Rect available, MutableMap<Window, Rect> result) {
        switch (tree) {
            case WindowTree.Leaf leaf -> result.put(leaf.window(), available);
            case WindowTree.Split split -> {
                if (split.direction() == Direction.HORIZONTAL) {
                    layoutHorizontal(split, available, result);
                } else {
                    layoutVertical(split, available, result);
                }
            }
        }
    }

    private static void layoutHorizontal(WindowTree.Split split, Rect available, MutableMap<Window, Rect> result) {
        int firstHeight = (int) (available.height() * split.ratio());
        // 最低でも1行（モードラインのみ）を確保
        firstHeight = Math.max(firstHeight, 1);
        int secondHeight = available.height() - firstHeight;
        secondHeight = Math.max(secondHeight, 1);
        // 合計が超過する場合は調整
        if (firstHeight + secondHeight > available.height()) {
            firstHeight = available.height() - secondHeight;
        }

        var firstRect = new Rect(available.top(), available.left(), available.width(), firstHeight);
        var secondRect = new Rect(available.top() + firstHeight, available.left(), available.width(), secondHeight);

        computeRecursive(split.first(), firstRect, result);
        computeRecursive(split.second(), secondRect, result);
    }

    private static void layoutVertical(WindowTree.Split split, Rect available, MutableMap<Window, Rect> result) {
        int firstWidth = (int) (available.width() * split.ratio());
        // 最低でも1カラムを確保
        firstWidth = Math.max(firstWidth, 1);
        // セパレータ分を引いた残り
        int secondWidth = available.width() - firstWidth - SEPARATOR_WIDTH;
        secondWidth = Math.max(secondWidth, 1);
        // 合計が超過する場合は調整
        if (firstWidth + SEPARATOR_WIDTH + secondWidth > available.width()) {
            firstWidth = available.width() - SEPARATOR_WIDTH - secondWidth;
        }

        var firstRect = new Rect(available.top(), available.left(), firstWidth, available.height());
        var secondRect = new Rect(
                available.top(), available.left() + firstWidth + SEPARATOR_WIDTH, secondWidth, available.height());

        computeRecursive(split.first(), firstRect, result);
        computeRecursive(split.second(), secondRect, result);
    }
}
