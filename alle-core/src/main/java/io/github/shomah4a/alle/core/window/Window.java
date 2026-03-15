package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.buffer.Buffer;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * バッファに対するビュー。
 * カーソル位置(point)、スクロール位置等のビュー固有の状態を持つ。
 * 同一バッファを複数のWindowで表示した場合、それぞれが独立した状態を持てる。
 */
public class Window {

    private Buffer buffer;
    private @Nullable Buffer previousBuffer;
    private int point;
    private int displayStartLine;
    private @Nullable Integer mark;

    public Window(Buffer buffer) {
        this.buffer = buffer;
        this.point = 0;
        this.displayStartLine = 0;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    /**
     * ウィンドウに表示するバッファを切り替える。
     * 切り替え前のバッファを直前バッファとして記録する。
     */
    public void setBuffer(Buffer buffer) {
        this.previousBuffer = this.buffer;
        this.buffer = buffer;
        this.point = 0;
        this.displayStartLine = 0;
        this.mark = null;
    }

    /**
     * 直前に表示していたバッファを返す。
     */
    public Optional<Buffer> getPreviousBuffer() {
        return Optional.ofNullable(previousBuffer);
    }

    /**
     * 直前バッファが指定バッファと同一の場合にクリアする。
     * バッファ削除時の dangling reference 防止用。
     */
    public void clearPreviousBufferIf(Buffer target) {
        if (previousBuffer == target) {
            previousBuffer = null;
        }
    }

    /**
     * カーソル位置(point)をコードポイント単位で返す。
     * バッファの長さを超過している場合は末尾にclampする。
     */
    public int getPoint() {
        int length = buffer.length();
        if (point > length) {
            point = length;
        }
        return point;
    }

    /**
     * カーソル位置(point)をコードポイント単位で設定する。
     *
     * @throws IndexOutOfBoundsException pointが範囲外の場合
     */
    public void setPoint(int point) {
        int length = buffer.length();
        if (point < 0 || point > length) {
            throw new IndexOutOfBoundsException("point " + point + " is out of bounds [0, " + length + "]");
        }
        this.point = point;
    }

    /**
     * 表示開始行を返す。
     */
    public int getDisplayStartLine() {
        return displayStartLine;
    }

    /**
     * 表示開始行を設定する。
     *
     * @throws IndexOutOfBoundsException lineが範囲外の場合
     */
    public void setDisplayStartLine(int line) {
        int lineCount = buffer.lineCount();
        if (line < 0 || line >= lineCount) {
            throw new IndexOutOfBoundsException("line " + line + " is out of bounds [0, " + lineCount + ")");
        }
        this.displayStartLine = line;
    }

    /**
     * カーソル位置に文字列を挿入し、カーソルを挿入文字列の後ろに移動する。
     */
    public void insert(String text) {
        int cursorBefore = point;
        var inverseChange = buffer.insertText(point, text);
        int insertedCodePoints = (int) text.codePoints().count();
        point += insertedCodePoints;
        buffer.getUndoManager().record(inverseChange, cursorBefore);
        buffer.markDirty();
    }

    /**
     * カーソルの手前からcount文字を削除する(バックスペース相当)。
     */
    public void deleteBackward(int count) {
        int deleteCount = Math.min(count, point);
        if (deleteCount == 0) {
            return;
        }
        int cursorBefore = point;
        int deleteStart = point - deleteCount;
        var inverseChange = buffer.deleteText(deleteStart, deleteCount);
        point = deleteStart;
        buffer.getUndoManager().record(inverseChange, cursorBefore);
        buffer.markDirty();
    }

    /**
     * カーソルの後ろからcount文字を削除する(Delete相当)。
     */
    public void deleteForward(int count) {
        int remaining = buffer.length() - point;
        int deleteCount = Math.min(count, remaining);
        if (deleteCount == 0) {
            return;
        }
        int cursorBefore = point;
        var inverseChange = buffer.deleteText(point, deleteCount);
        buffer.getUndoManager().record(inverseChange, cursorBefore);
        buffer.markDirty();
    }

    /**
     * markを設定する。
     */
    public void setMark(int mark) {
        int length = buffer.length();
        if (mark < 0 || mark > length) {
            throw new IndexOutOfBoundsException("mark " + mark + " is out of bounds [0, " + length + "]");
        }
        this.mark = mark;
    }

    /**
     * markをクリアする。
     */
    public void clearMark() {
        this.mark = null;
    }

    /**
     * markを返す。バッファ長を超過している場合はclampする。
     */
    public Optional<Integer> getMark() {
        if (mark == null) {
            return Optional.empty();
        }
        int length = buffer.length();
        if (mark > length) {
            mark = length;
        }
        return Optional.of(mark);
    }

    /**
     * regionの開始位置（markとpointの小さい方）を返す。
     * markが未設定の場合はempty。
     */
    public Optional<Integer> getRegionStart() {
        return getMark().map(m -> Math.min(m, getPoint()));
    }

    /**
     * regionの終了位置（markとpointの大きい方）を返す。
     * markが未設定の場合はempty。
     */
    public Optional<Integer> getRegionEnd() {
        return getMark().map(m -> Math.max(m, getPoint()));
    }
}
