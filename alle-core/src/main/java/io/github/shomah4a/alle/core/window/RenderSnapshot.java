package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.styling.StyledSpan;
import java.util.Optional;
import org.eclipse.collections.api.list.ListIterable;

/**
 * 描画に必要な全情報を保持するimmutableなスナップショット。
 * Frameの状態を読み取って構築し、描画スレッドに渡す用途を想定。
 */
public record RenderSnapshot(
        int screenCols,
        int screenRows,
        ListIterable<WindowSnapshot> windowSnapshots,
        ListIterable<Separator> separators,
        MinibufferSnapshot minibuffer,
        int cursorColumn,
        int cursorRow) {

    /**
     * 各ウィンドウの描画データ。
     */
    public record WindowSnapshot(
            Rect rect, ListIterable<LineSnapshot> visibleLines, int displayStartColumn, String modeLine) {}

    /**
     * 1行分の描画データ。
     */
    public record LineSnapshot(String text, Optional<ListIterable<StyledSpan>> spans) {}

    /**
     * ミニバッファ / エコーエリアの描画データ。
     */
    public record MinibufferSnapshot(Optional<String> text, int displayStartColumn) {}
}
