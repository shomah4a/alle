package io.github.shomah4a.alle.core.window;

import org.jspecify.annotations.Nullable;

/**
 * ウィンドウのビュー固有の状態のスナップショット。
 * バッファ切り替え時にカーソル位置やスクロール状態を保存・復元するために使用する。
 */
public record ViewState(
        int point,
        int displayStartLine,
        int displayStartVisualLine,
        int displayStartColumn,
        @Nullable Integer mark) {

    /** すべての値がゼロ/null の初期状態。 */
    public static final ViewState INITIAL = new ViewState(0, 0, 0, 0, null);
}
