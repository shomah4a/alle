package io.github.shomah4a.alle.core.buffer;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * テキストプロパティの範囲管理を行う。
 * 各エントリは半開区間 [start, end) で管理される。
 * rear-nonsticky: 範囲末尾位置への挿入はプロパティに含まれない。
 */
class TextPropertyStore {

    private final MutableList<Entry> entries = Lists.mutable.empty();

    /**
     * 指定範囲にread-onlyを設定する。
     */
    void putReadOnly(int start, int end) {
        // 重複エントリを除去
        entries.removeIf(e -> e.readOnly && overlaps(e.start, e.end, start, end));
        entries.add(new Entry(start, end, true));
    }

    /**
     * 指定範囲のread-onlyを解除する。
     * 範囲が完全に含まれるエントリを除去し、部分的に重なるエントリは縮小する。
     */
    void removeReadOnly(int start, int end) {
        var toRemove = Lists.mutable.<Entry>empty();
        var toAdd = Lists.mutable.<Entry>empty();

        for (var entry : entries) {
            if (!entry.readOnly) {
                continue;
            }
            if (entry.start >= start && entry.end <= end) {
                // 完全に含まれる: 除去
                toRemove.add(entry);
            } else if (entry.start < start && entry.end > end) {
                // 指定範囲がエントリの中央: 2つに分割
                toRemove.add(entry);
                toAdd.add(entry.withRange(entry.start, start));
                toAdd.add(entry.withRange(end, entry.end));
            } else if (entry.start < end && entry.end > end) {
                // 左側が重なる: 開始位置を調整
                toRemove.add(entry);
                toAdd.add(entry.withRange(end, entry.end));
            } else if (entry.start < start && entry.end > start) {
                // 右側が重なる: 終了位置を調整
                toRemove.add(entry);
                toAdd.add(entry.withRange(entry.start, start));
            }
        }

        entries.removeAll(toRemove);
        entries.addAll(toAdd);
    }

    /**
     * 指定位置がread-onlyかどうかを返す。
     * 半開区間 [start, end) で判定する（rear-nonsticky）。
     */
    boolean isReadOnly(int index) {
        for (var entry : entries) {
            if (entry.readOnly && entry.start <= index && index < entry.end) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定範囲にread-onlyプロパティが設定されているかチェックする。
     * 範囲 [start, start + count) 内のいずれかの位置にread-onlyがあればtrue。
     */
    boolean hasReadOnly(int start, int count) {
        for (var entry : entries) {
            if (entry.readOnly && entry.start < start + count && entry.end > start) {
                return true;
            }
        }
        return false;
    }

    /**
     * テキスト挿入時の範囲調整。
     * 挿入位置より後の範囲をシフトする。
     * rear-nonsticky: 挿入位置がエントリのend位置と一致する場合はシフトのみ（範囲拡大しない）。
     */
    void adjustForInsert(int index, int length) {
        var adjusted = Lists.mutable.<Entry>empty();
        for (var entry : entries) {
            if (entry.end <= index) {
                // 挿入位置より前: 変更なし
                adjusted.add(entry);
            } else if (entry.start > index) {
                // 挿入位置より後: 全体をシフト
                adjusted.add(entry.withRange(entry.start + length, entry.end + length));
            } else if (entry.start <= index && index < entry.end) {
                // 挿入位置がエントリ内部: 範囲を拡大（rear-nonsticky: endと一致する場合はここに来ない）
                adjusted.add(entry.withRange(entry.start, entry.end + length));
            } else {
                // entry.end == index: rear-nonsticky。endをシフトしない
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
        var adjusted = Lists.mutable.<Entry>empty();
        for (var entry : entries) {
            if (entry.end <= index) {
                // 削除範囲より前: 変更なし
                adjusted.add(entry);
            } else if (entry.start >= deleteEnd) {
                // 削除範囲より後: シフト
                adjusted.add(entry.withRange(entry.start - count, entry.end - count));
            } else if (entry.start >= index && entry.end <= deleteEnd) {
                // 完全に削除範囲内: 除去
            } else if (entry.start < index && entry.end > deleteEnd) {
                // 削除範囲がエントリ内部: 縮小
                adjusted.add(entry.withRange(entry.start, entry.end - count));
            } else if (entry.start < index) {
                // エントリの後半が削除される
                adjusted.add(entry.withRange(entry.start, index));
            } else {
                // エントリの前半が削除される
                adjusted.add(entry.withRange(index, entry.end - count));
            }
        }
        entries.clear();
        entries.addAll(adjusted);
    }

    /**
     * 全エントリを除去する。
     */
    void clear() {
        entries.clear();
    }

    private static boolean overlaps(int s1, int e1, int s2, int e2) {
        return s1 < e2 && s2 < e1;
    }

    private record Entry(int start, int end, boolean readOnly) {

        Entry withRange(int newStart, int newEnd) {
            return new Entry(newStart, newEnd, readOnly);
        }
    }
}
