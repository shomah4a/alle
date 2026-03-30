package io.github.shomah4a.alle.core.mode.modes.occur;

/**
 * occurのマッチ結果1行分を表す。
 *
 * @param lineIndex 元バッファの行番号（0始まり）
 * @param lineText 行テキスト（改行を含まない）
 * @param matchOffsetInLine 行内のマッチ開始位置（コードポイント単位）
 */
public record OccurMatch(int lineIndex, String lineText, int matchOffsetInLine) {}
