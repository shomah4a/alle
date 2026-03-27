package io.github.shomah4a.alle.core.buffer;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * 半開区間 [start, end) の範囲を複数管理するデータ構造。
 * テキストプロパティの範囲管理に使用する。
 * rear-nonsticky: 範囲末尾位置への挿入はプロパティに含まれない。
 */
class RangeList {

    private final MutableList<Range> ranges = Lists.mutable.empty();

    /**
     * 指定範囲を追加する。重複する既存範囲は除去される。
     */
    void put(int start, int end) {
        ranges.removeIf(r -> overlaps(r.start, r.end, start, end));
        ranges.add(new Range(start, end));
    }

    /**
     * 指定範囲を除去する。
     * 完全に含まれるエントリは除去し、部分的に重なるエントリは縮小する。
     */
    void remove(int start, int end) {
        var toRemove = Lists.mutable.<Range>empty();
        var toAdd = Lists.mutable.<Range>empty();

        for (var range : ranges) {
            if (range.start >= start && range.end <= end) {
                toRemove.add(range);
            } else if (range.start < start && range.end > end) {
                toRemove.add(range);
                toAdd.add(new Range(range.start, start));
                toAdd.add(new Range(end, range.end));
            } else if (range.start < end && range.end > end) {
                toRemove.add(range);
                toAdd.add(new Range(end, range.end));
            } else if (range.start < start && range.end > start) {
                toRemove.add(range);
                toAdd.add(new Range(range.start, start));
            }
        }

        ranges.removeAll(toRemove);
        ranges.addAll(toAdd);
    }

    /**
     * 指定位置が範囲内かどうかを返す。
     * 半開区間 [start, end) で判定する（rear-nonsticky）。
     */
    boolean contains(int index) {
        for (var range : ranges) {
            if (range.start <= index && index < range.end) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定範囲 [start, start + count) 内にいずれかの範囲が存在するかチェックする。
     */
    boolean hasAny(int start, int count) {
        for (var range : ranges) {
            if (range.start < start + count && range.end > start) {
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
        var adjusted = Lists.mutable.<Range>empty();
        for (var range : ranges) {
            if (range.end <= index) {
                adjusted.add(range);
            } else if (range.start > index) {
                adjusted.add(new Range(range.start + length, range.end + length));
            } else if (range.start <= index && index < range.end) {
                adjusted.add(new Range(range.start, range.end + length));
            } else {
                adjusted.add(range);
            }
        }
        ranges.clear();
        ranges.addAll(adjusted);
    }

    /**
     * テキスト削除時の範囲調整。
     * 削除範囲に応じてエントリを縮小・除去する。
     */
    void adjustForDelete(int index, int count) {
        int deleteEnd = index + count;
        var adjusted = Lists.mutable.<Range>empty();
        for (var range : ranges) {
            if (range.end <= index) {
                adjusted.add(range);
            } else if (range.start >= deleteEnd) {
                adjusted.add(new Range(range.start - count, range.end - count));
            } else if (range.start >= index && range.end <= deleteEnd) {
                // 完全に削除範囲内: 除去
            } else if (range.start < index && range.end > deleteEnd) {
                adjusted.add(new Range(range.start, range.end - count));
            } else if (range.start < index) {
                adjusted.add(new Range(range.start, index));
            } else {
                adjusted.add(new Range(index, range.end - count));
            }
        }
        ranges.clear();
        ranges.addAll(adjusted);
    }

    /**
     * 指定位置を含む範囲のstart位置を返す。
     * 範囲内でなければ-1を返す。
     */
    int findStart(int index) {
        for (var range : ranges) {
            if (range.start <= index && index < range.end) {
                return range.start;
            }
        }
        return -1;
    }

    /**
     * 指定位置を含む範囲のend位置を返す。
     * 範囲内でなければ-1を返す。
     */
    int findEnd(int index) {
        for (var range : ranges) {
            if (range.start <= index && index < range.end) {
                return range.end;
            }
        }
        return -1;
    }

    /**
     * 全範囲を除去する。
     */
    void clear() {
        ranges.clear();
    }

    private static boolean overlaps(int s1, int e1, int s2, int e2) {
        return s1 < e2 && s2 < e1;
    }

    private record Range(int start, int end) {}
}
