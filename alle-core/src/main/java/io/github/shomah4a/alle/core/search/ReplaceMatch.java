package io.github.shomah4a.alle.core.search;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 置換検索のマッチ結果。
 * start/end はコードポイント単位のオフセット（end は排他的）。
 */
public sealed interface ReplaceMatch {

    int start();

    int end();

    /**
     * リテラル検索のマッチ。
     */
    record Literal(int start, int end) implements ReplaceMatch {}

    /**
     * 正規表現検索のマッチ。
     * groups.get(0) はマッチ全体、groups.get(n) は n 番目のキャプチャグループ。
     * 存在しないキャプチャ（null）は空文字列に正規化された状態で格納される。
     */
    record Regex(int start, int end, ListIterable<String> groups) implements ReplaceMatch {}
}
