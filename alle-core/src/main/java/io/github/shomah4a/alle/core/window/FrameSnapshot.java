package io.github.shomah4a.alle.core.window;

import java.util.Objects;

/**
 * フレーム全体の状態スナップショット。
 * ウィンドウツリーの構造と、アクティブウィンドウの位置を保持する。
 *
 * @param tree ウィンドウツリーのスナップショット
 * @param activeWindowIndex アクティブウィンドウの深さ優先順インデックス
 */
public record FrameSnapshot(WindowTreeSnapshot tree, int activeWindowIndex) {

    public FrameSnapshot {
        Objects.requireNonNull(tree);
        if (activeWindowIndex < 0) {
            throw new IllegalArgumentException("activeWindowIndex must be non-negative: " + activeWindowIndex);
        }
    }
}
