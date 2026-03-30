package io.github.shomah4a.alle.core.search;

/**
 * バッファ内テキスト検索の結果。
 * start, endはコードポイント単位のオフセット。
 *
 * @param start マッチ開始位置（コードポイント単位）
 * @param end マッチ終了位置（コードポイント単位、排他的）
 * @param wrapped ラップアラウンドで見つかったかどうか
 */
public record SearchResult(int start, int end, boolean wrapped) {}
