package io.github.shomah4a.alle.core.util;

/**
 * 文字列マッチングのユーティリティ。
 *
 * <p>ケース無視比較は {@link String#regionMatches(boolean, int, String, int, int)} 準拠の
 * Locale 非依存 case folding である。BMP 内の単一 char ↔ 単一 char 対応のケース折りに限定し、
 * Turkish-I（{@code İ} ↔ {@code i}）のような Locale 依存 / 1 文字 ↔ 複数文字対応
 * （例: {@code ß} ↔ {@code SS}）の Unicode full case folding は対象外。
 */
public final class StringMatching {

    private StringMatching() {}

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

    /**
     * クエリに大文字またはタイトルケース文字が 1 つでも含まれているかを判定する。
     *
     * <p>smart-case 判定（emacs の {@code isearch-no-upper-case-p} 相当）に用いる。
     * {@link Character#isUpperCase(int)} と {@link Character#isTitleCase(int)} の OR で評価する。
     * 合字の Title Case 文字（例: {@code ǅ}）も大文字扱いとする。
     *
     * <p>ASCII の大文字以外にもギリシャ大文字・キリル大文字なども true を返す点に注意。
     *
     * @param query 判定対象の文字列
     * @return 大文字またはタイトルケース文字を含むなら true
     */
    public static boolean containsUpperCase(String query) {
        return query.codePoints().anyMatch(cp -> Character.isUpperCase(cp) || Character.isTitleCase(cp));
    }

    /**
     * 部分文字列の前方検索を行う。{@link String#indexOf(String, int)} 相当だが
     * {@code ignoreCase = true} のときケース無視で比較する。
     *
     * <p>戻り値は <strong>char offset</strong>（コードポイントオフセットではない）。
     *
     * <p>{@code ignoreCase = true} の挙動:
     * <ul>
     *   <li>{@link String#regionMatches(boolean, int, String, int, int)} を線形に走査する</li>
     *   <li>計算量は O(n × m)（n: text 長、m: query 長）</li>
     *   <li>サロゲートペアの下位サロゲート位置からはマッチを開始しない（コードポイント境界を尊重）</li>
     * </ul>
     *
     * @param text       検索対象文字列
     * @param query      検索クエリ
     * @param fromIndex  検索開始位置（char offset）。負の値は 0 として扱う
     * @param ignoreCase true ならケース無視で比較する
     * @return マッチした位置の char offset。マッチなしまたは {@code query} が空文字なら -1
     */
    public static int indexOf(String text, String query, int fromIndex, boolean ignoreCase) {
        if (query.isEmpty()) {
            return -1;
        }
        if (!ignoreCase) {
            return text.indexOf(query, fromIndex);
        }
        int textLen = text.length();
        int queryLen = query.length();
        int start = Math.max(0, fromIndex);
        int maxStart = textLen - queryLen;
        for (int i = start; i <= maxStart; i++) {
            if (Character.isLowSurrogate(text.charAt(i))) {
                continue;
            }
            if (text.regionMatches(true, i, query, 0, queryLen)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 部分文字列の後方検索を行う。{@link String#lastIndexOf(String, int)} 相当だが
     * {@code ignoreCase = true} のときケース無視で比較する。
     *
     * <p>戻り値は <strong>char offset</strong>（コードポイントオフセットではない）。
     *
     * <p>{@code fromIndex} の意味は {@link String#lastIndexOf(String, int)} と同じで、
     * マッチの開始位置がこの値以下となる最大位置を返す。
     *
     * <p>{@code ignoreCase = true} の挙動:
     * <ul>
     *   <li>{@link String#regionMatches(boolean, int, String, int, int)} を線形に走査する</li>
     *   <li>計算量は O(n × m)（n: text 長、m: query 長）</li>
     *   <li>サロゲートペアの下位サロゲート位置からはマッチを開始しない（コードポイント境界を尊重）</li>
     * </ul>
     *
     * @param text       検索対象文字列
     * @param query      検索クエリ
     * @param fromIndex  検索開始位置（char offset）。マッチ開始位置はこの値以下となる
     * @param ignoreCase true ならケース無視で比較する
     * @return マッチした位置の char offset。マッチなしまたは {@code query} が空文字なら -1
     */
    public static int lastIndexOf(String text, String query, int fromIndex, boolean ignoreCase) {
        if (query.isEmpty()) {
            return -1;
        }
        if (!ignoreCase) {
            return text.lastIndexOf(query, fromIndex);
        }
        int textLen = text.length();
        int queryLen = query.length();
        if (fromIndex < 0) {
            return -1;
        }
        int start = Math.min(fromIndex, textLen - queryLen);
        for (int i = start; i >= 0; i--) {
            if (Character.isLowSurrogate(text.charAt(i))) {
                continue;
            }
            if (text.regionMatches(true, i, query, 0, queryLen)) {
                return i;
            }
        }
        return -1;
    }
}
