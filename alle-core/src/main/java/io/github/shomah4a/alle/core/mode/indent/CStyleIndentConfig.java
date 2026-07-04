package io.github.shomah4a.alle.core.mode.indent;

import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Cスタイルインデントの設定。
 * 開き括弧文字・閉じ括弧文字・コメントとみなすノード型をカスタマイズ可能にする。
 *
 * <p>コメントノード型は言語ごとに異なる（例: JavaScript/TypeScript/Terraform は
 * {@code comment}、Java は {@code line_comment} / {@code block_comment}）ため、
 * 括弧文字と同様に言語ごとに明示的に注入する（ADR 0142 参照。ADR 0115 の部分変更）。
 *
 * @param indentWidth インデント幅（スペース数）
 * @param openBrackets 開き括弧とみなす文字の集合
 * @param closeBrackets 閉じ括弧とみなす文字の集合
 * @param commentNodeTypes コメントとみなす構文木ノード型名の集合
 */
public record CStyleIndentConfig(
        int indentWidth,
        ImmutableSet<Character> openBrackets,
        ImmutableSet<Character> closeBrackets,
        ImmutableSet<String> commentNodeTypes) {}
