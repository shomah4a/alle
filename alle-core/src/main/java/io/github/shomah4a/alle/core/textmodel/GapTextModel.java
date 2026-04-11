package io.github.shomah4a.alle.core.textmodel;

import io.github.shomah4a.alle.libs.gapbuffer.GapModel;
import org.eclipse.collections.api.factory.primitive.IntLists;
import org.eclipse.collections.api.list.primitive.MutableIntList;

/**
 * {@link GapModel}をラップしてコードポイント単位の操作を提供する{@link TextModel}実装。
 */
public class GapTextModel implements TextModel {

    private final GapModel gapModel;

    /**
     * 改行文字('\n')の論理charオフセットを昇順で保持するキャッシュ。
     * insert/delete時に差分更新され、行操作メソッドの高速化に使用する。
     */
    private final MutableIntList lineBreakOffsets;

    public GapTextModel() {
        this.gapModel = new GapModel();
        this.lineBreakOffsets = IntLists.mutable.empty();
    }

    public GapTextModel(GapModel gapModel) {
        this.gapModel = gapModel;
        this.lineBreakOffsets = buildLineBreakCache(gapModel);
    }

    /**
     * GapModelの全文を走査して改行位置キャッシュを構築する。
     */
    private static MutableIntList buildLineBreakCache(GapModel model) {
        MutableIntList offsets = IntLists.mutable.empty();
        int len = model.length();
        int pos = 0;
        while (pos < len) {
            int found = model.indexOf('\n', pos, len);
            if (found < 0) {
                break;
            }
            offsets.add(found);
            pos = found + 1;
        }
        return offsets;
    }

    @Override
    public int length() {
        return Character.codePointCount(new CharSequenceView(), 0, gapModel.length());
    }

    @Override
    public int codePointAt(int index) {
        int charOffset = toCharOffset(index);
        return Character.codePointAt(new CharSequenceView(), charOffset);
    }

    @Override
    public void insert(int index, String text) {
        int charOffset = toCharOffsetForInsert(index);
        gapModel.insert(charOffset, text);
        updateCacheAfterInsert(charOffset, text);
    }

    @Override
    public void delete(int index, int count) {
        if (count == 0) {
            return;
        }
        int charStart = toCharOffset(index);
        int charEnd = toCharOffsetForInsert(index + count);
        updateCacheBeforeDelete(charStart, charEnd - charStart);
        gapModel.delete(charStart, charEnd - charStart);
    }

    /**
     * 挿入後にキャッシュを差分更新する。
     *
     * @param charOffset 挿入された論理charオフセット
     * @param text       挿入されたテキスト
     */
    private void updateCacheAfterInsert(int charOffset, String text) {
        int textLen = text.length();
        if (textLen == 0) {
            return;
        }

        // 挿入位置以降の既存改行オフセットをシフト
        for (int i = 0; i < lineBreakOffsets.size(); i++) {
            if (lineBreakOffsets.get(i) >= charOffset) {
                lineBreakOffsets.set(i, lineBreakOffsets.get(i) + textLen);
            }
        }

        // 挿入テキスト内の改行を検索してキャッシュに追加
        int insertIndex = findInsertionPoint(charOffset);
        int added = 0;
        for (int i = 0; i < textLen; i++) {
            if (text.charAt(i) == '\n') {
                lineBreakOffsets.addAtIndex(insertIndex + added, charOffset + i);
                added++;
            }
        }
    }

    /**
     * 削除前にキャッシュを差分更新する。
     * GapModel.delete の前に呼び出す必要がある（削除範囲の改行情報が必要なため）。
     *
     * @param charOffset 削除開始の論理charオフセット
     * @param charCount  削除するchar数
     */
    private void updateCacheBeforeDelete(int charOffset, int charCount) {
        if (charCount == 0) {
            return;
        }
        int deleteEnd = charOffset + charCount;

        // 削除範囲内の改行を除去し、削除範囲後の改行オフセットをシフト
        int writeIdx = 0;
        for (int readIdx = 0; readIdx < lineBreakOffsets.size(); readIdx++) {
            int offset = lineBreakOffsets.get(readIdx);
            if (offset >= charOffset && offset < deleteEnd) {
                // 削除範囲内 — スキップ
                continue;
            }
            int newOffset = offset >= deleteEnd ? offset - charCount : offset;
            lineBreakOffsets.set(writeIdx, newOffset);
            writeIdx++;
        }
        // 末尾の余分な要素を除去
        while (lineBreakOffsets.size() > writeIdx) {
            lineBreakOffsets.removeAtIndex(lineBreakOffsets.size() - 1);
        }
    }

    /**
     * 指定charオフセット以上の最初の要素のインデックスを返す（二分探索）。
     */
    private int findInsertionPoint(int charOffset) {
        int lo = 0;
        int hi = lineBreakOffsets.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (lineBreakOffsets.get(mid) < charOffset) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    @Override
    public String substring(int start, int end) {
        if (start == end) {
            return "";
        }
        int charStart = toCharOffsetForInsert(start);
        int charEnd = toCharOffsetForInsert(end);
        return gapModel.substring(charStart, charEnd);
    }

    @Override
    public int lineCount() {
        return lineBreakOffsets.size() + 1;
    }

    @Override
    public int lineIndexForOffset(int offset) {
        int cpLen = length();
        if (offset < 0 || offset > cpLen) {
            throw new IndexOutOfBoundsException("offset: " + offset + ", length: " + cpLen);
        }
        if (offset == cpLen) {
            return lineCount() - 1;
        }
        int charOffset = toCharOffset(offset);
        // キャッシュを二分探索してcharOffsetより前にある改行の数を求める
        int lo = 0;
        int hi = lineBreakOffsets.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (lineBreakOffsets.get(mid) < charOffset) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        // lo = charOffset以上の最初の改行インデックス = charOffsetが属する行番号
        // 改行位置上のオフセットもその行の末尾として扱われる（lo番目の改行 = lo番目の行の終端）
        return lo;
    }

    @Override
    public int lineStartOffset(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lineCount()) {
            throw new IndexOutOfBoundsException("lineIndex: " + lineIndex + ", lineCount: " + lineCount());
        }
        if (lineIndex == 0) {
            return 0;
        }
        // lineIndex番目の行の先頭 = (lineIndex-1)番目の改行の次の文字
        int charOffset = lineBreakOffsets.get(lineIndex - 1) + 1;
        return charOffsetToCodePointIndex(charOffset);
    }

    @Override
    public String lineText(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lineCount()) {
            throw new IndexOutOfBoundsException("lineIndex: " + lineIndex + ", lineCount: " + lineCount());
        }
        int lineCharStart = lineIndex == 0 ? 0 : lineBreakOffsets.get(lineIndex - 1) + 1;
        int lineCharEnd = lineIndex < lineBreakOffsets.size() ? lineBreakOffsets.get(lineIndex) : gapModel.length();
        return gapModel.substring(lineCharStart, lineCharEnd);
    }

    @Override
    public String getText() {
        return gapModel.toString();
    }

    /**
     * コードポイントインデックスをcharオフセットに変換する。
     * 既存の位置を指す場合に使用（codePointAt, delete等）。
     */
    private int toCharOffset(int codePointIndex) {
        int charLen = gapModel.length();
        int cpLen = length();
        if (codePointIndex < 0 || codePointIndex >= cpLen) {
            throw new IndexOutOfBoundsException(
                    "codePointIndex " + codePointIndex + " is out of bounds [0, " + cpLen + ")");
        }
        return offsetCodePoints(0, codePointIndex, charLen);
    }

    /**
     * コードポイントインデックスをcharオフセットに変換する。
     * 挿入位置を指す場合に使用（insert, substring等）。末尾位置も許容する。
     */
    private int toCharOffsetForInsert(int codePointIndex) {
        int cpLen = length();
        if (codePointIndex < 0 || codePointIndex > cpLen) {
            throw new IndexOutOfBoundsException(
                    "codePointIndex " + codePointIndex + " is out of bounds [0, " + cpLen + "]");
        }
        if (codePointIndex == cpLen) {
            return gapModel.length();
        }
        return offsetCodePoints(0, codePointIndex, gapModel.length());
    }

    /**
     * charオフセットからコードポイントインデックスに変換する。
     */
    private int charOffsetToCodePointIndex(int charOffset) {
        return Character.codePointCount(new CharSequenceView(), 0, charOffset);
    }

    /**
     * 指定されたchar位置から指定コードポイント数だけ進んだchar位置を返す。
     */
    private int offsetCodePoints(int charStart, int codePointCount, int charLimit) {
        int pos = charStart;
        for (int i = 0; i < codePointCount; i++) {
            if (pos >= charLimit) {
                throw new IndexOutOfBoundsException("Exceeded char limit");
            }
            pos += Character.charCount(Character.codePointAt(new CharSequenceView(), pos));
        }
        return pos;
    }

    /**
     * GapModelをCharSequenceとして参照するためのビュー。
     * Character.codePointAt等のAPIに渡すために使用する。
     */
    private class CharSequenceView implements CharSequence {

        @Override
        public int length() {
            return gapModel.length();
        }

        @Override
        public char charAt(int index) {
            return gapModel.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return gapModel.substring(start, end);
        }
    }
}
