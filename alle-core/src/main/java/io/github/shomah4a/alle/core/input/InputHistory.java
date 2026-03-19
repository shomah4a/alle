package io.github.shomah4a.alle.core.input;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * ミニバッファの入力履歴を保持するデータストア。
 * 確定した入力文字列を時系列順に蓄積する。
 * ナビゲーション状態は持たない（{@link HistoryNavigator} が担当する）。
 * 全メソッドはスレッドセーフである。
 */
public class InputHistory {

    private static final int DEFAULT_MAX_SIZE = 100;

    private final MutableList<String> entries;
    private final int maxSize;

    public InputHistory() {
        this(DEFAULT_MAX_SIZE);
    }

    public InputHistory(int maxSize) {
        this.entries = Lists.mutable.empty();
        this.maxSize = maxSize;
    }

    /**
     * 履歴に入力を追加する。
     * 空文字列は無視する。既に同じ文字列が存在する場合は最新位置に移動する。
     */
    public synchronized void add(String entry) {
        if (entry.isEmpty()) {
            return;
        }
        entries.remove(entry);
        entries.add(entry);
        if (entries.size() > maxSize) {
            entries.remove(0);
        }
    }

    /**
     * 指定インデックスの履歴を取得する。0が最古、size()-1が最新。
     */
    public synchronized String get(int index) {
        return entries.get(index);
    }

    /**
     * 履歴のサイズを返す。
     */
    public synchronized int size() {
        return entries.size();
    }
}
