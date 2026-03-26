package io.github.shomah4a.alle.core.input;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 補完候補から補完結果を算出する。
 */
public final class CompletionResult {

    private CompletionResult() {}

    /**
     * 候補リストから最長共通プレフィックスを算出する。
     * 候補が0件なら入力をそのまま返す。
     * 候補が1件なら候補そのものを返す。
     * 候補が複数なら最長共通プレフィックスを返す。
     *
     * @param input      現在の入力文字列
     * @param candidates 補完候補
     * @return 補完後の文字列
     */
    public static String resolve(String input, ListIterable<CompletionCandidate> candidates) {
        if (candidates.isEmpty()) {
            return input;
        }
        if (candidates.size() == 1) {
            return candidates.get(0).value();
        }
        return longestCommonPrefix(candidates.collect(CompletionCandidate::value));
    }

    /**
     * 候補リストから補完結果の詳細を算出する。
     * 候補の有無・数に応じて{@link CompletionOutcome}のバリアントを返す。
     *
     * @param input      現在の入力文字列
     * @param candidates 補完候補
     * @return 補完結果の詳細
     */
    public static CompletionOutcome resolveDetailed(String input, ListIterable<CompletionCandidate> candidates) {
        if (candidates.isEmpty()) {
            return new CompletionOutcome.NoMatch();
        }
        if (candidates.size() == 1) {
            return new CompletionOutcome.Unique(candidates.get(0));
        }
        var values = candidates.collect(CompletionCandidate::value);
        return new CompletionOutcome.Partial(longestCommonPrefix(values), candidates);
    }

    /**
     * 文字列リストの最長共通プレフィックスを返す。
     */
    static String longestCommonPrefix(ListIterable<String> strings) {
        String first = strings.get(0);
        int prefixLen = first.length();
        for (int i = 1; i < strings.size(); i++) {
            String s = strings.get(i);
            prefixLen = Math.min(prefixLen, s.length());
            for (int j = 0; j < prefixLen; j++) {
                if (first.charAt(j) != s.charAt(j)) {
                    prefixLen = j;
                    break;
                }
            }
        }
        return first.substring(0, prefixLen);
    }
}
