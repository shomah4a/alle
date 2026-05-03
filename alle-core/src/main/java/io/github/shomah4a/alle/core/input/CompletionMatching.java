package io.github.shomah4a.alle.core.input;

/**
 * 補完候補のマッチングに用いるユーティリティ。
 *
 * <p>ケース無視比較は {@link String#regionMatches(boolean, int, String, int, int)} ベースの
 * ASCII 互換 case folding（{@link Character#toLowerCase(char)} ベース）であり、
 * Locale 非依存である。Turkish-I（{@code İ} ↔ {@code i}）のような Unicode case folding は
 * 対象外である点に注意。
 */
public final class CompletionMatching {

    private CompletionMatching() {}

    /**
     * {@code str} が {@code prefix} で始まるかをケース無視で判定する。
     *
     * @param str    対象文字列
     * @param prefix プレフィックス
     * @return ケース無視で前方一致するなら true
     */
    public static boolean startsWithIgnoreCase(String str, String prefix) {
        return str.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * 設定値に応じて、ケース敏感／無視を切り替えて前方一致を判定する。
     *
     * @param str        対象文字列
     * @param prefix     プレフィックス
     * @param ignoreCase true ならケース無視で比較する
     */
    public static boolean startsWith(String str, String prefix, boolean ignoreCase) {
        return ignoreCase ? startsWithIgnoreCase(str, prefix) : str.startsWith(prefix);
    }
}
