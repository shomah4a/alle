package io.github.shomah4a.alle.core.input;

import org.eclipse.collections.api.list.ListIterable;

/**
 * 補完結果の詳細を表す。
 * 候補の有無・数に応じて異なるバリアントを返す。
 */
public sealed interface CompletionOutcome {

    /**
     * 候補が1件で一意に確定した。
     *
     * @param value 確定した補完文字列
     */
    record Unique(String value) implements CompletionOutcome {}

    /**
     * 候補が複数あり、最長共通プレフィックスまで部分補完した。
     *
     * @param commonPrefix 最長共通プレフィックス
     * @param candidates   補完候補のリスト
     */
    record Partial(String commonPrefix, ListIterable<String> candidates) implements CompletionOutcome {}

    /**
     * 候補が0件で補完できなかった。
     */
    record NoMatch() implements CompletionOutcome {}
}
