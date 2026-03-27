package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.styling.Face;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * face付き半開区間 [start, end) の範囲を複数管理するデータ構造。
 * テキストプロパティのface属性に使用する。
 * rear-nonsticky: 範囲末尾位置への挿入はプロパティに含まれない。
 */
class FaceRangeList {

    private final MutableList<FaceRange> ranges = Lists.mutable.empty();

    /**
     * 指定範囲にfaceを設定する。
     * 重複する既存範囲は分割され、非重複部分は元のfaceで保持される。
     */
    void put(int start, int end, Face face) {
        remove(start, end);
        ranges.add(new FaceRange(start, end, face));
    }

    /**
     * 指定範囲のfaceを除去する。
     * 完全に含まれるエントリは除去し、部分的に重なるエントリは縮小する。
     */
    void remove(int start, int end) {
        var toRemove = Lists.mutable.<FaceRange>empty();
        var toAdd = Lists.mutable.<FaceRange>empty();

        for (var range : ranges) {
            if (range.start >= start && range.end <= end) {
                toRemove.add(range);
            } else if (range.start < start && range.end > end) {
                toRemove.add(range);
                toAdd.add(new FaceRange(range.start, start, range.face));
                toAdd.add(new FaceRange(end, range.end, range.face));
            } else if (range.start < end && range.end > end) {
                toRemove.add(range);
                toAdd.add(new FaceRange(end, range.end, range.face));
            } else if (range.start < start && range.end > start) {
                toRemove.add(range);
                toAdd.add(new FaceRange(range.start, start, range.face));
            }
        }

        ranges.removeAll(toRemove);
        ranges.addAll(toAdd);
    }

    /**
     * 指定範囲 [queryStart, queryEnd) 内にあるface範囲をStyledSpanリストとして返す。
     * 返されるStyledSpanの座標はバッファ先頭からのコードポイントオフセット。
     */
    ListIterable<StyledSpan> getFaceSpans(int queryStart, int queryEnd) {
        MutableList<StyledSpan> result = Lists.mutable.empty();
        for (var range : ranges) {
            if (range.start < queryEnd && range.end > queryStart) {
                int clampedStart = Math.max(range.start, queryStart);
                int clampedEnd = Math.min(range.end, queryEnd);
                result.add(new StyledSpan(clampedStart, clampedEnd, range.face));
            }
        }
        result.sortThisBy(StyledSpan::start);
        return result;
    }

    /**
     * テキスト挿入時の範囲調整。
     * rear-nonsticky: 範囲末尾位置への挿入では範囲が拡大しない。
     */
    void adjustForInsert(int index, int length) {
        var adjusted = Lists.mutable.<FaceRange>empty();
        for (var range : ranges) {
            if (range.end <= index) {
                adjusted.add(range);
            } else if (range.start > index) {
                adjusted.add(new FaceRange(range.start + length, range.end + length, range.face));
            } else if (range.start <= index && index < range.end) {
                adjusted.add(new FaceRange(range.start, range.end + length, range.face));
            } else {
                adjusted.add(range);
            }
        }
        ranges.clear();
        ranges.addAll(adjusted);
    }

    /**
     * テキスト削除時の範囲調整。
     */
    void adjustForDelete(int index, int count) {
        int deleteEnd = index + count;
        var adjusted = Lists.mutable.<FaceRange>empty();
        for (var range : ranges) {
            if (range.end <= index) {
                adjusted.add(range);
            } else if (range.start >= deleteEnd) {
                adjusted.add(new FaceRange(range.start - count, range.end - count, range.face));
            } else if (range.start >= index && range.end <= deleteEnd) {
                // 完全に削除範囲内: 除去
            } else if (range.start < index && range.end > deleteEnd) {
                adjusted.add(new FaceRange(range.start, range.end - count, range.face));
            } else if (range.start < index) {
                adjusted.add(new FaceRange(range.start, index, range.face));
            } else {
                adjusted.add(new FaceRange(index, range.end - count, range.face));
            }
        }
        ranges.clear();
        ranges.addAll(adjusted);
    }

    /**
     * 全範囲を除去する。
     */
    void clear() {
        ranges.clear();
    }

    private record FaceRange(int start, int end, Face face) {}
}
