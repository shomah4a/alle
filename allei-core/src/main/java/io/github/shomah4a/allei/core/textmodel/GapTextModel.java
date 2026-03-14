package io.github.shomah4a.allei.core.textmodel;

import io.github.shomah4a.allei.libs.gapbuffer.GapModel;

/**
 * {@link GapModel}をラップしてコードポイント単位の操作を提供する{@link TextModel}実装。
 */
public class GapTextModel implements TextModel {

    private final GapModel gapModel;

    public GapTextModel() {
        this.gapModel = new GapModel();
    }

    public GapTextModel(GapModel gapModel) {
        this.gapModel = gapModel;
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
    }

    @Override
    public void delete(int index, int count) {
        if (count == 0) {
            return;
        }
        int charStart = toCharOffset(index);
        int charEnd = toCharOffsetForInsert(index + count);
        gapModel.delete(charStart, charEnd - charStart);
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
        int charLen = gapModel.length();
        if (charLen == 0) {
            return 1;
        }
        int count = 1;
        for (int i = 0; i < charLen; i++) {
            if (gapModel.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }

    @Override
    public int lineStartOffset(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lineCount()) {
            throw new IndexOutOfBoundsException("lineIndex: " + lineIndex + ", lineCount: " + lineCount());
        }
        if (lineIndex == 0) {
            return 0;
        }
        int charLen = gapModel.length();
        int currentLine = 0;
        for (int i = 0; i < charLen; i++) {
            if (gapModel.charAt(i) == '\n') {
                currentLine++;
                if (currentLine == lineIndex) {
                    return charOffsetToCodePointIndex(i + 1);
                }
            }
        }
        throw new IndexOutOfBoundsException("lineIndex: " + lineIndex);
    }

    @Override
    public String lineText(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lineCount()) {
            throw new IndexOutOfBoundsException("lineIndex: " + lineIndex + ", lineCount: " + lineCount());
        }
        int charLen = gapModel.length();
        int currentLine = 0;
        int lineStart = 0;
        for (int i = 0; i < charLen; i++) {
            if (gapModel.charAt(i) == '\n') {
                if (currentLine == lineIndex) {
                    return gapModel.substring(lineStart, i);
                }
                currentLine++;
                lineStart = i + 1;
            }
        }
        if (currentLine == lineIndex) {
            return gapModel.substring(lineStart, charLen);
        }
        throw new IndexOutOfBoundsException("lineIndex: " + lineIndex);
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
