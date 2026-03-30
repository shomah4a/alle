package io.github.shomah4a.alle.core.buffer;

import java.util.Objects;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * 値付き半開区間 [start, end) の範囲を複数管理するデータ構造。
 * テキストプロパティの範囲管理に使用する。
 * rear-nonsticky: 範囲末尾位置への挿入はプロパティに含まれない。
 *
 * <p>値を持たない範囲管理には {@link RangeList.Flag} を型パラメータとして使用し、
 * {@link #put(int, int, Object)} に {@link Flag#ON} を渡して追加する。
 *
 * @param <V> 各範囲に関連付ける値の型。
 */
class RangeList<V> {

    /**
     * 値を持たない範囲管理に使用するマーカー型。
     */
    enum Flag {
        ON
    }

    private final MutableList<Entry<V>> entries = Lists.mutable.empty();

    /**
     * 指定範囲に値を設定する。
     * 重複する既存範囲は分割され、非重複部分は元の値で保持される。
     * 隣接する同値エントリはマージされる。
     */
    void put(int start, int end, V value) {
        remove(start, end);
        entries.add(new Entry<>(start, end, value));
        mergeAdjacent(value);
    }

    /**
     * 指定範囲を除去する。
     * 完全に含まれるエントリは除去し、部分的に重なるエントリは縮小する。
     */
    void remove(int start, int end) {
        MutableList<Entry<V>> toRemove = Lists.mutable.empty();
        MutableList<Entry<V>> toAdd = Lists.mutable.empty();

        for (var entry : entries) {
            if (entry.start >= start && entry.end <= end) {
                toRemove.add(entry);
            } else if (entry.start < start && entry.end > end) {
                toRemove.add(entry);
                toAdd.add(new Entry<>(entry.start, start, entry.value));
                toAdd.add(new Entry<>(end, entry.end, entry.value));
            } else if (entry.start < end && entry.end > end) {
                toRemove.add(entry);
                toAdd.add(new Entry<>(end, entry.end, entry.value));
            } else if (entry.start < start && entry.end > start) {
                toRemove.add(entry);
                toAdd.add(new Entry<>(entry.start, start, entry.value));
            }
        }

        entries.removeAll(toRemove);
        entries.addAll(toAdd);
    }

    /**
     * 指定位置が範囲内かどうかを返す。
     * 半開区間 [start, end) で判定する（rear-nonsticky）。
     */
    boolean contains(int index) {
        for (var entry : entries) {
            if (entry.start <= index && index < entry.end) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定範囲 [start, start + count) 内にいずれかの範囲が存在するかチェックする。
     */
    boolean hasAny(int start, int count) {
        for (var entry : entries) {
            if (entry.start < start + count && entry.end > start) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定範囲 [queryStart, queryEnd) 内にあるエントリをリストとして返す。
     * 返されるエントリの座標はクエリ範囲にクランプされる。
     * 結果はstart順にソートされる。
     */
    ListIterable<Entry<V>> getEntries(int queryStart, int queryEnd) {
        MutableList<Entry<V>> result = Lists.mutable.empty();
        for (var entry : entries) {
            if (entry.start < queryEnd && entry.end > queryStart) {
                int clampedStart = Math.max(entry.start, queryStart);
                int clampedEnd = Math.min(entry.end, queryEnd);
                result.add(new Entry<>(clampedStart, clampedEnd, entry.value));
            }
        }
        result.sortThisBy(Entry::start);
        return result;
    }

    /**
     * テキスト挿入時の範囲調整。
     * 挿入位置より後の範囲をシフトする。
     * rear-nonsticky: 挿入位置がエントリのend位置と一致する場合はシフトのみ（範囲拡大しない）。
     */
    void adjustForInsert(int index, int length) {
        MutableList<Entry<V>> adjusted = Lists.mutable.empty();
        for (var entry : entries) {
            if (entry.end <= index) {
                adjusted.add(entry);
            } else if (entry.start > index) {
                adjusted.add(new Entry<>(entry.start + length, entry.end + length, entry.value));
            } else if (entry.start <= index && index < entry.end) {
                adjusted.add(new Entry<>(entry.start, entry.end + length, entry.value));
            } else {
                adjusted.add(entry);
            }
        }
        entries.clear();
        entries.addAll(adjusted);
    }

    /**
     * テキスト削除時の範囲調整。
     * 削除範囲に応じてエントリを縮小・除去する。
     */
    void adjustForDelete(int index, int count) {
        int deleteEnd = index + count;
        MutableList<Entry<V>> adjusted = Lists.mutable.empty();
        for (var entry : entries) {
            if (entry.end <= index) {
                adjusted.add(entry);
            } else if (entry.start >= deleteEnd) {
                adjusted.add(new Entry<>(entry.start - count, entry.end - count, entry.value));
            } else if (entry.start >= index && entry.end <= deleteEnd) {
                // 完全に削除範囲内: 除去
            } else if (entry.start < index && entry.end > deleteEnd) {
                adjusted.add(new Entry<>(entry.start, entry.end - count, entry.value));
            } else if (entry.start < index) {
                adjusted.add(new Entry<>(entry.start, index, entry.value));
            } else {
                adjusted.add(new Entry<>(index, entry.end - count, entry.value));
            }
        }
        entries.clear();
        entries.addAll(adjusted);
    }

    /**
     * 指定位置を含む範囲のstart位置を返す。
     * 範囲内でなければ-1を返す。
     */
    int findStart(int index) {
        for (var entry : entries) {
            if (entry.start <= index && index < entry.end) {
                return entry.start;
            }
        }
        return -1;
    }

    /**
     * 指定位置を含む範囲のend位置を返す。
     * 範囲内でなければ-1を返す。
     */
    int findEnd(int index) {
        for (var entry : entries) {
            if (entry.start <= index && index < entry.end) {
                return entry.end;
            }
        }
        return -1;
    }

    /**
     * 指定範囲内で指定値を持つエントリのみを除去する。
     * 他の値を持つエントリには影響しない。
     */
    void removeByValue(int start, int end, V value) {
        MutableList<Entry<V>> toRemove = Lists.mutable.empty();
        MutableList<Entry<V>> toAdd = Lists.mutable.empty();

        for (var entry : entries) {
            if (!Objects.equals(entry.value, value)) {
                continue;
            }
            if (entry.start >= start && entry.end <= end) {
                toRemove.add(entry);
            } else if (entry.start < start && entry.end > end) {
                toRemove.add(entry);
                toAdd.add(new Entry<>(entry.start, start, entry.value));
                toAdd.add(new Entry<>(end, entry.end, entry.value));
            } else if (entry.start < end && entry.end > end) {
                toRemove.add(entry);
                toAdd.add(new Entry<>(end, entry.end, entry.value));
            } else if (entry.start < start && entry.end > start) {
                toRemove.add(entry);
                toAdd.add(new Entry<>(entry.start, start, entry.value));
            }
        }

        entries.removeAll(toRemove);
        entries.addAll(toAdd);
    }

    /**
     * 全範囲を除去する。
     */
    void clear() {
        entries.clear();
    }

    /**
     * 隣接または重複する同値エントリをマージする。
     */
    private void mergeAdjacent(V value) {
        MutableList<Entry<V>> matching = Lists.mutable.empty();
        MutableList<Entry<V>> others = Lists.mutable.empty();

        for (var entry : entries) {
            if (Objects.equals(entry.value, value)) {
                matching.add(entry);
            } else {
                others.add(entry);
            }
        }

        if (matching.size() <= 1) {
            return;
        }

        matching.sortThisBy(Entry::start);

        MutableList<Entry<V>> merged = Lists.mutable.empty();
        Entry<V> current = matching.get(0);
        for (int i = 1; i < matching.size(); i++) {
            Entry<V> next = matching.get(i);
            if (next.start <= current.end) {
                current = new Entry<>(current.start, Math.max(current.end, next.end), value);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);

        entries.clear();
        entries.addAll(others);
        entries.addAll(merged);
    }

    record Entry<V>(int start, int end, V value) {}
}
