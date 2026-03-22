package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 1行分のスタイリング結果。スパンのリストと次行に渡す状態のペア。
 *
 * @param spans スタイルスパンのリスト
 * @param nextState 次行に引き継ぐ状態
 */
public record StylingResult(ListIterable<StyledSpan> spans, StylingState nextState) {}
