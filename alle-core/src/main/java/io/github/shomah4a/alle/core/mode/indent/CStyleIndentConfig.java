package io.github.shomah4a.alle.core.mode.indent;

import java.util.regex.Pattern;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Cスタイルインデントの設定。
 * 開き括弧文字・閉じ括弧文字をカスタマイズ可能にする。
 *
 * @param indentWidth インデント幅（スペース数）
 * @param openBrackets 開き括弧とみなす文字の集合
 * @param closeBrackets 閉じ括弧とみなす文字の集合
 * @param openBracketEndPattern 行末が開き括弧で終わるかを判定するパターン
 * @param closeBracketStartPattern 行頭（空白後）が閉じ括弧で始まるかを判定するパターン
 */
public record CStyleIndentConfig(
        int indentWidth,
        ImmutableSet<Character> openBrackets,
        ImmutableSet<Character> closeBrackets,
        Pattern openBracketEndPattern,
        Pattern closeBracketStartPattern) {

    /**
     * 開き括弧・閉じ括弧文字を指定してConfigを生成するファクトリ。
     * パターンは自動的に構築される。
     *
     * @param indentWidth インデント幅
     * @param openBrackets 開き括弧文字の集合
     * @param closeBrackets 閉じ括弧文字の集合
     * @return 設定
     */
    public static CStyleIndentConfig of(
            int indentWidth, ImmutableSet<Character> openBrackets, ImmutableSet<Character> closeBrackets) {
        String openCharsEscaped = buildCharClassContent(openBrackets);
        String closeCharsEscaped = buildCharClassContent(closeBrackets);
        Pattern openEnd = Pattern.compile("[" + openCharsEscaped + "]\\s*(?://.*|/\\*.*)?$");
        Pattern closeStart = Pattern.compile("^\\s*[" + closeCharsEscaped + "]");
        return new CStyleIndentConfig(indentWidth, openBrackets, closeBrackets, openEnd, closeStart);
    }

    private static String buildCharClassContent(ImmutableSet<Character> chars) {
        var sb = new StringBuilder();
        for (char c : chars.toSortedList()) {
            if (c == '[' || c == ']' || c == '\\' || c == '^' || c == '-') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }
}
