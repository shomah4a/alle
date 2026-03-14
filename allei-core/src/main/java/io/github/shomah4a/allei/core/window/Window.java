package io.github.shomah4a.allei.core.window;

import io.github.shomah4a.allei.core.buffer.Buffer;

/**
 * バッファに対するビュー。
 * カーソル位置(point)、スクロール位置等のビュー固有の状態を持つ。
 * 同一バッファを複数のWindowで表示した場合、それぞれが独立した状態を持てる。
 */
public class Window {

    private Buffer buffer;
    private int point;
    private int displayStartLine;

    public Window(Buffer buffer) {
        this.buffer = buffer;
        this.point = 0;
        this.displayStartLine = 0;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public void setBuffer(Buffer buffer) {
        this.buffer = buffer;
        this.point = 0;
        this.displayStartLine = 0;
    }

    /**
     * カーソル位置(point)をコードポイント単位で返す。
     * バッファの長さを超過している場合は末尾にclampする。
     */
    public int getPoint() {
        int length = buffer.getTextModel().length();
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
        int length = buffer.getTextModel().length();
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
        int lineCount = buffer.getTextModel().lineCount();
        if (line < 0 || line >= lineCount) {
            throw new IndexOutOfBoundsException("line " + line + " is out of bounds [0, " + lineCount + ")");
        }
        this.displayStartLine = line;
    }

    /**
     * カーソル位置に文字列を挿入し、カーソルを挿入文字列の後ろに移動する。
     */
    public void insert(String text) {
        buffer.getTextModel().insert(point, text);
        int insertedCodePoints = (int) text.codePoints().count();
        point += insertedCodePoints;
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
        int deleteStart = point - deleteCount;
        buffer.getTextModel().delete(deleteStart, deleteCount);
        point = deleteStart;
        buffer.markDirty();
    }

    /**
     * カーソルの後ろからcount文字を削除する(Delete相当)。
     */
    public void deleteForward(int count) {
        int remaining = buffer.getTextModel().length() - point;
        int deleteCount = Math.min(count, remaining);
        if (deleteCount == 0) {
            return;
        }
        buffer.getTextModel().delete(point, deleteCount);
        buffer.markDirty();
    }
}
