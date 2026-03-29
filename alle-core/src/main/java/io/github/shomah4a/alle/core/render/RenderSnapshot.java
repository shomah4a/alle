package io.github.shomah4a.alle.core.render;

import io.github.shomah4a.alle.core.styling.StyledSpan;
import io.github.shomah4a.alle.core.window.Rect;
import io.github.shomah4a.alle.core.window.Separator;
import java.util.Optional;
import java.util.OptionalInt;
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
        CursorPosition cursorPosition) {

    /**
     * リージョンの範囲（コードポイント単位のバッファオフセット）。
     */
    public record RegionRange(int start, int end) {}

    /**
     * 各ウィンドウの描画データ。
     *
     * @param highlightLine ハイライト行（visibleLines内の相対インデックス）。未設定はempty。
     * @param regionRange リージョン範囲。マーク未設定時はempty。
     */
    public record WindowSnapshot(
            Rect rect,
            ListIterable<LineSnapshot> visibleLines,
            int displayStartColumn,
            String modeLine,
            OptionalInt highlightLine,
            Optional<RegionRange> regionRange) {}

    /**
     * 行内のリージョン範囲（コードポイント単位の行ローカルオフセット）。
     */
    public record LineRegion(int startCp, int endCp) {}

    /**
     * 1行分の描画データ。
     *
     * @param regionInLine 行内のリージョン範囲。リージョン外の場合はempty。
     */
    public record LineSnapshot(
            String text, Optional<ListIterable<StyledSpan>> spans, Optional<LineRegion> regionInLine) {}

    /**
     * ミニバッファ / エコーエリアの描画データ。
     *
     * @param spans テキストプロパティfaceによるスタイル情報。未設定はempty。
     */
    public record MinibufferSnapshot(
            Optional<String> text, int displayStartColumn, Optional<ListIterable<StyledSpan>> spans) {}
}
