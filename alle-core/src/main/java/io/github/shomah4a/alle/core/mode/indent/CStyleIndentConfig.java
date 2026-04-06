package io.github.shomah4a.alle.core.mode.indent;

import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Cスタイルインデントの設定。
 * 開き括弧文字・閉じ括弧文字をカスタマイズ可能にする。
 *
 * @param indentWidth インデント幅（スペース数）
 * @param openBrackets 開き括弧とみなす文字の集合
 * @param closeBrackets 閉じ括弧とみなす文字の集合
 */
public record CStyleIndentConfig(
        int indentWidth, ImmutableSet<Character> openBrackets, ImmutableSet<Character> closeBrackets) {}
