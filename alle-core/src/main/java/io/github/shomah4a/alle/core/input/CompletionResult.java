package io.github.shomah4a.alle.core.input;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 補完候補から補完結果を算出する。
 */
public final class CompletionResult {

    private CompletionResult() {}

    /**
     * 候補リストから補完結果の詳細を算出する（ケース敏感）。
     */
    public static CompletionOutcome resolveDetailed(String input, ListIterable<CompletionCandidate> candidates) {
        return resolveDetailed(input, candidates, false);
    }

    /**
     * 候補リストから補完結果の詳細を算出する。
     * 候補の有無・数に応じて{@link CompletionOutcome}のバリアントを返す。
     *
     * @param input      現在の入力文字列
     * @param candidates 補完候補
     * @param ignoreCase true なら最長共通プレフィックスをケース無視で算出する
     * @return 補完結果の詳細
     */
    public static CompletionOutcome resolveDetailed(
            String input, ListIterable<CompletionCandidate> candidates, boolean ignoreCase) {
        if (candidates.isEmpty()) {
            return new CompletionOutcome.NoMatch();
        }
        if (candidates.size() == 1) {
            return new CompletionOutcome.Unique(candidates.get(0));
        }
        var values = candidates.collect(CompletionCandidate::value);
        return new CompletionOutcome.Partial(longestCommonPrefix(values, ignoreCase), candidates);
    }

    /**
     * 文字列リストの最長共通プレフィックスを返す（ケース敏感）。
     */
    static String longestCommonPrefix(ListIterable<String> strings) {
        return longestCommonPrefix(strings, false);
    }

    /**
     * 文字列リストの最長共通プレフィックスを返す。
     *
     * <p>{@code ignoreCase} が true の場合、比較はケース無視で行うが、戻り値は
     * 先頭候補からそのまま {@link String#substring(int, int)} で取り出すため、
     * 候補のケース表記がそのまま反映される。
     *
     * <p>サロゲートペアを跨いで切れないよう、進行は codePoint 単位で行う。
     *
     * @param strings    文字列リスト（1件以上）
     * @param ignoreCase true ならケース無視で比較する
     */
    static String longestCommonPrefix(ListIterable<String> strings, boolean ignoreCase) {
        String first = strings.get(0);
        int prefixCharLen = first.length();
        for (int i = 1; i < strings.size(); i++) {
            String s = strings.get(i);
            prefixCharLen = commonPrefixCharLength(first, s, prefixCharLen, ignoreCase);
            if (prefixCharLen == 0) {
                break;
            }
        }
        return first.substring(0, prefixCharLen);
    }

    /**
     * 2つの文字列の共通プレフィックス長（char 単位、codePoint 境界に揃う）を返す。
     *
     * @param a       文字列1
     * @param b       文字列2
     * @param maxChar 比較する最大 char 長
     */
    private static int commonPrefixCharLength(String a, String b, int maxChar, boolean ignoreCase) {
        int limit = Math.min(maxChar, Math.min(a.length(), b.length()));
        int i = 0;
        while (i < limit) {
            int cpA = a.codePointAt(i);
            int cpB = b.codePointAt(i);
            int charCount = Character.charCount(cpA);
            if (Character.charCount(cpB) != charCount) {
                break;
            }
            if (i + charCount > limit) {
                break;
            }
            if (cpA != cpB) {
                if (!ignoreCase) {
                    break;
                }
                if (Character.toLowerCase(cpA) != Character.toLowerCase(cpB)
                        && Character.toUpperCase(cpA) != Character.toUpperCase(cpB)) {
                    break;
                }
            }
            i += charCount;
        }
        return i;
    }
}
