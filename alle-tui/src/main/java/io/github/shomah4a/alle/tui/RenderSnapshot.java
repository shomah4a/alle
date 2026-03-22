package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.TerminalPosition;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import io.github.shomah4a.alle.core.window.Rect;
import io.github.shomah4a.alle.core.window.Separator;
import java.util.Optional;
import org.eclipse.collections.api.list.ListIterable;

/**
 * 描画に必要な全情報を保持するimmutableなスナップショット。
 * Frameの状態を読み取って構築し、描画スレッドに渡す用途を想定。
 */
record RenderSnapshot(
        int screenCols,
        int screenRows,
        ListIterable<WindowSnapshot> windowSnapshots,
        ListIterable<Separator> separators,
        MinibufferSnapshot minibuffer,
        TerminalPosition cursorPosition) {

    /**
     * 各ウィンドウの描画データ。
     */
    record WindowSnapshot(
            Rect rect, ListIterable<LineSnapshot> visibleLines, int displayStartColumn, String modeLine) {}

    /**
     * 1行分の描画データ。
     */
    record LineSnapshot(String text, Optional<ListIterable<StyledSpan>> spans) {}

    /**
     * ミニバッファ / エコーエリアの描画データ。
     */
    record MinibufferSnapshot(Optional<String> text, int displayStartColumn) {}
}
