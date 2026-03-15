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
}
