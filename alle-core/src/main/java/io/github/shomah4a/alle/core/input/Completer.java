package io.github.shomah4a.alle.core.input;

import org.eclipse.collections.api.list.ListIterable;

/**
 * ミニバッファ入力の補完候補を提供する。
 * 入力文字列に対して前方一致する候補のリストを返す。
 */
@FunctionalInterface
public interface Completer {

    /**
     * 入力文字列に対する補完候補を返す。
     *
     * @param input 現在の入力文字列
     * @return 補完候補のリスト
     */
    ListIterable<CompletionCandidate> complete(String input);

    /**
     * 入力文字列に対する補完候補をlabelの自然順でソートして返す。
     * 表示用途など候補の順序が重要な場面ではこちらを使用する。
     * このメソッドはオーバーライドせず、そのまま使用すること。
     *
     * @param input 現在の入力文字列
     * @return ソート済みの補完候補のリスト
     */
    default ListIterable<CompletionCandidate> sortedComplete(String input) {
        return complete(input).toSortedListBy(CompletionCandidate::label);
    }
}
