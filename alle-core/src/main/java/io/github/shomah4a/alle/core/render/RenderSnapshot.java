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
     * @param truncateLines trueの場合は切り詰めモード（水平スクロール）、falseは折り返しモード。
     * @param highlightLine ハイライト行（visibleLines内の相対インデックス）。未設定はempty。
     * @param regionRange リージョン範囲。マーク未設定時はempty。
     * @param tabWidth タブストップ間隔（カラム数）。
     */
    public record WindowSnapshot(
            Rect rect,
            ListIterable<LineSnapshot> visibleLines,
            int displayStartColumn,
            boolean truncateLines,
            String modeLine,
            OptionalInt highlightLine,
            Optional<RegionRange> regionRange,
            int tabWidth) {}

    /**
     * 行内のリージョン範囲（コードポイント単位の行ローカルオフセット）。
     */
    public record LineRegion(int startCp, int endCp) {}

    /**
     * 視覚行の描画範囲（コードポイント単位の行ローカルオフセット）。
     * 折り返しモード時に、バッファ行テキストのどの部分を描画するかを示す。
     * 切り詰めモード時はemptyで、displayStartColumnとウィンドウ幅で描画範囲が決まる。
     */
    public record VisualLineRange(int startCp, int endCp) {}

    /**
     * 1行分の描画データ。
     *
     * @param text バッファ行のテキスト全体（折り返し時も元バッファ行のテキスト全体を保持）。
     * @param spans スタイル情報（行ローカルオフセット基準）。
     * @param regionInLine 行内のリージョン範囲。リージョン外の場合はempty。
     * @param visualLineRange 視覚行の描画範囲。折り返しモード時のみ設定。
     */
    public record LineSnapshot(
            String text,
            Optional<ListIterable<StyledSpan>> spans,
            Optional<LineRegion> regionInLine,
            Optional<VisualLineRange> visualLineRange) {

        /**
         * 切り詰めモード用のファクトリ（visualLineRange なし）。
         */
        public static LineSnapshot truncated(
                String text, Optional<ListIterable<StyledSpan>> spans, Optional<LineRegion> regionInLine) {
            return new LineSnapshot(text, spans, regionInLine, Optional.empty());
        }

        /**
         * 折り返しモード用のファクトリ（visualLineRange あり）。
         */
        public static LineSnapshot wrapped(
                String text,
                Optional<ListIterable<StyledSpan>> spans,
                Optional<LineRegion> regionInLine,
                int startCp,
                int endCp) {
            return new LineSnapshot(text, spans, regionInLine, Optional.of(new VisualLineRange(startCp, endCp)));
        }
    }

    /**
     * ミニバッファ / エコーエリアの描画データ。
     *
     * @param spans テキストプロパティfaceによるスタイル情報。未設定はempty。
     */
    public record MinibufferSnapshot(
            Optional<String> text, int displayStartColumn, Optional<ListIterable<StyledSpan>> spans, int tabWidth) {}
}
