package io.github.shomah4a.alle.core.highlight;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 1行分のハイライト結果。スパンのリストと次行に渡す状態のペア。
 *
 * @param spans ハイライトスパンのリスト
 * @param nextState 次行に引き継ぐ状態
 */
public record HighlightResult(ListIterable<StyledSpan> spans, HighlightState nextState) {}
