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

    /**
     * 入力文字列のシャドウ境界位置を返す。
     * シャドウとは、新しいルート指定によって無効化された先行部分のこと。
     * 戻り値は有効入力の開始位置であり、0の場合はシャドウなし。
     * ファイルパス補完など、シャドウが必要な場合にオーバーライドする。
     *
     * @param input 現在の入力文字列
     * @return シャドウ境界位置（有効入力の開始インデックス）
     */
    default int shadowBoundary(String input) {
        return 0;
    }
}
