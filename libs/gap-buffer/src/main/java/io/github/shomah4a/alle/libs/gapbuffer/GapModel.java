package io.github.shomah4a.alle.libs.gapbuffer;

/**
 * Gap Bufferによるテキストデータ構造。
 * 内部的にchar配列を保持し、カーソル位置付近の挿入・削除をO(1)で行う。
 *
 * <p>不変条件:
 * <ul>
 *   <li>{@code 0 <= gapStart <= gapEnd <= buffer.length}</li>
 *   <li>論理的なテキスト長 = {@code buffer.length - (gapEnd - gapStart)}</li>
 * </ul>
 */
public class GapModel {

    private static final int DEFAULT_GAP_SIZE = 64;

    private char[] buffer;
    private int gapStart;
    private int gapEnd;

    public GapModel() {
        this(DEFAULT_GAP_SIZE);
    }

    public GapModel(int initialGapSize) {
        if (initialGapSize < 0) {
            throw new IllegalArgumentException("initialGapSize must be non-negative: " + initialGapSize);
        }
        this.buffer = new char[initialGapSize];
        this.gapStart = 0;
        this.gapEnd = initialGapSize;
    }

    /**
     * テキストの論理的な長さを返す。
     */
    public int length() {
        return buffer.length - gapSize();
    }

    /**
     * 指定位置の文字を返す。
     *
     * @throws IndexOutOfBoundsException offsetが範囲外の場合
     */
    public char charAt(int offset) {
        checkOffset(offset, length());
        return buffer[toPhysical(offset)];
    }

    /**
     * 指定位置に文字列を挿入する。
     *
     * @throws IndexOutOfBoundsException offsetが範囲外の場合
     */
    public void insert(int offset, String text) {
        if (text.isEmpty()) {
            return;
        }
        checkOffset(offset, length() + 1);
        moveGap(offset);
        ensureGapCapacity(text.length());
        text.getChars(0, text.length(), buffer, gapStart);
        gapStart += text.length();
    }

    /**
     * 指定位置に1文字挿入する。
     *
     * @throws IndexOutOfBoundsException offsetが範囲外の場合
     */
    public void insert(int offset, char c) {
        checkOffset(offset, length() + 1);
        moveGap(offset);
        ensureGapCapacity(1);
        buffer[gapStart] = c;
        gapStart++;
    }

    /**
     * 指定位置から指定文字数を削除する。
     *
     * @throws IndexOutOfBoundsException 範囲が不正な場合
     */
    public void delete(int offset, int count) {
        if (count == 0) {
            return;
        }
        if (count < 0) {
            throw new IndexOutOfBoundsException("count must be non-negative: " + count);
        }
        int len = length();
        checkOffset(offset, len);
        if (offset + count > len) {
            throw new IndexOutOfBoundsException(
                    "offset + count exceeds length: " + offset + " + " + count + " > " + len);
        }
        moveGap(offset);
        gapEnd += count;
    }

    /**
     * 指定範囲の部分文字列を返す。
     *
     * @throws IndexOutOfBoundsException 範囲が不正な場合
     */
    public String substring(int start, int end) {
        int len = length();
        if (start < 0 || end < start || end > len) {
            throw new IndexOutOfBoundsException("Invalid range: [" + start + ", " + end + ") for length " + len);
        }
        if (start == end) {
            return "";
        }

        // ギャップをまたがない場合
        if (end <= gapStart || start >= gapStart) {
            int physStart = toPhysical(start);
            return new String(buffer, physStart, end - start);
        }

        // ギャップをまたぐ場合
        StringBuilder sb = new StringBuilder(end - start);
        sb.append(buffer, start, gapStart - start);
        sb.append(buffer, gapEnd, end - gapStart);
        return sb.toString();
    }

    @Override
    public String toString() {
        return substring(0, length());
    }

    private int gapSize() {
        return gapEnd - gapStart;
    }

    private int toPhysical(int logicalOffset) {
        return logicalOffset < gapStart ? logicalOffset : logicalOffset + gapSize();
    }

    private void moveGap(int offset) {
        if (offset == gapStart) {
            return;
        }
        if (offset < gapStart) {
            int moveCount = gapStart - offset;
            System.arraycopy(buffer, offset, buffer, gapEnd - moveCount, moveCount);
            gapEnd -= moveCount;
            gapStart = offset;
        } else {
            int moveCount = offset - gapStart;
            System.arraycopy(buffer, gapEnd, buffer, gapStart, moveCount);
            gapStart += moveCount;
            gapEnd += moveCount;
        }
    }

    private void ensureGapCapacity(int required) {
        if (gapSize() >= required) {
            return;
        }
        int newGapSize = Math.max(required, buffer.length);
        char[] newBuffer = new char[buffer.length + newGapSize];
        System.arraycopy(buffer, 0, newBuffer, 0, gapStart);
        int afterGapLength = buffer.length - gapEnd;
        System.arraycopy(buffer, gapEnd, newBuffer, newBuffer.length - afterGapLength, afterGapLength);
        gapEnd = newBuffer.length - afterGapLength;
        buffer = newBuffer;
    }

    private static void checkOffset(int offset, int upperBound) {
        if (offset < 0 || offset >= upperBound) {
            throw new IndexOutOfBoundsException("offset " + offset + " is out of bounds [0, " + upperBound + ")");
        }
    }
}
