package io.github.shomah4a.alle.core.highlight;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 行テキストからハイライト情報を生成するインターフェース。
 * 返されるStyledSpanリストはstart順にソートされ、重複がないことを保証する。
 */
public interface SyntaxHighlighter {

    /**
     * 行テキストをハイライトしてStyledSpanのリストを返す。
     * スパンに含まれないコードポイントはデフォルトスタイルで描画される。
     */
    ListIterable<StyledSpan> highlight(String lineText);

    /**
     * 前行からの状態を受け取り、行テキストをハイライトして結果と次行の状態を返す。
     * マルチラインハイライトに対応する場合はこのメソッドをオーバーライドする。
     */
    default HighlightResult highlightLine(String lineText, HighlightState state) {
        return new HighlightResult(highlight(lineText), HighlightState.NONE);
    }

    /**
     * ハイライトの初期状態を返す。
     */
    default HighlightState initialState() {
        return HighlightState.NONE;
    }
}
