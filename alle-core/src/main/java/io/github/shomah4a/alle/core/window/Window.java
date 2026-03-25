package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * バッファに対するビュー。
 * カーソル位置(point)、スクロール位置等のビュー固有の状態を持つ。
 * 同一バッファを複数のWindowで表示した場合、それぞれが独立した状態を持てる。
 * バッファへのアクセスはBufferFacade経由で行い、読み取り専用チェックが適用される。
 */
public class Window {

    private BufferFacade bufferFacade;
    private @Nullable BufferFacade previousBuffer;
    private int point;
    private int displayStartLine;
    private int displayStartColumn;
    private @Nullable Integer mark;

    public Window(BufferFacade buffer) {
        this.bufferFacade = buffer;
        this.point = 0;
        this.displayStartLine = 0;
    }

    /**
     * バッファをBufferFacade経由で返す。
     * 読み取り専用バッファの場合、書き込み操作時にReadOnlyBufferExceptionがスローされる。
     */
    public BufferFacade getBuffer() {
        return bufferFacade;
    }

    /**
     * ウィンドウに表示するバッファを切り替える。
     * 切り替え前のバッファを直前バッファとして記録する。
     */
    public void setBuffer(BufferFacade buffer) {
        this.previousBuffer = this.bufferFacade;
        this.bufferFacade = buffer;
        this.point = 0;
        this.displayStartLine = 0;
        this.displayStartColumn = 0;
        this.mark = null;
    }

    /**
     * 直前に表示していたバッファを返す。
     */
    public Optional<BufferFacade> getPreviousBuffer() {
        return Optional.ofNullable(previousBuffer);
    }

    /**
     * 直前バッファが指定バッファと同一の場合にクリアする。
     * バッファ削除時の dangling reference 防止用。
     */
    public void clearPreviousBufferIf(BufferFacade target) {
        if (previousBuffer != null && target.equals(previousBuffer)) {
            previousBuffer = null;
        }
    }

    /**
     * カーソル位置(point)をコードポイント単位で返す。
     * バッファの長さを超過している場合は末尾にclampする。
     */
    public int getPoint() {
        int length = bufferFacade.length();
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
        int length = bufferFacade.length();
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
        int lineCount = bufferFacade.lineCount();
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
        var inverseChange = bufferFacade.insertText(point, text);
        int insertedCodePoints = (int) text.codePoints().count();
        point += insertedCodePoints;
        bufferFacade.getUndoManager().record(inverseChange, cursorBefore);
        bufferFacade.markDirty();
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
        var inverseChange = bufferFacade.deleteText(deleteStart, deleteCount);
        point = deleteStart;
        bufferFacade.getUndoManager().record(inverseChange, cursorBefore);
        bufferFacade.markDirty();
    }

    /**
     * カーソルの後ろからcount文字を削除する(Delete相当)。
     */
    public void deleteForward(int count) {
        int remaining = bufferFacade.length() - point;
        int deleteCount = Math.min(count, remaining);
        if (deleteCount == 0) {
            return;
        }
        int cursorBefore = point;
        var inverseChange = bufferFacade.deleteText(point, deleteCount);
        bufferFacade.getUndoManager().record(inverseChange, cursorBefore);
        bufferFacade.markDirty();
    }

    /**
     * カーソル位置が表示範囲内に収まるようにdisplayStartLineを調整する。
     *
     * @param visibleRows 表示可能な行数
     */
    public void ensurePointVisible(int visibleRows) {
        if (visibleRows <= 0) {
            return;
        }
        int currentLine = bufferFacade.lineIndexForOffset(getPoint());
        if (currentLine < displayStartLine) {
            displayStartLine = currentLine;
        } else if (currentLine >= displayStartLine + visibleRows) {
            displayStartLine = currentLine - visibleRows + 1;
        }
    }

    /**
     * 水平方向の表示開始カラムを返す。
     */
    public int getDisplayStartColumn() {
        return displayStartColumn;
    }

    /**
     * カーソル位置が水平方向の表示範囲内に収まるようにdisplayStartColumnを調整する。
     *
     * @param visibleColumns 表示可能なカラム数
     */
    public void ensurePointHorizontallyVisible(int visibleColumns) {
        if (visibleColumns <= 0) {
            return;
        }
        int currentPoint = getPoint();
        int lineIndex = bufferFacade.lineIndexForOffset(currentPoint);
        int lineStart = bufferFacade.lineStartOffset(lineIndex);
        String lineText = bufferFacade.lineText(lineIndex);
        int cursorColumn = DisplayWidthUtil.computeColumnForOffset(lineText, currentPoint - lineStart);

        if (cursorColumn < displayStartColumn) {
            displayStartColumn = DisplayWidthUtil.snapColumnToCharBoundary(lineText, cursorColumn);
        } else if (cursorColumn >= displayStartColumn + visibleColumns) {
            int newStart = cursorColumn - visibleColumns + 1;
            displayStartColumn = DisplayWidthUtil.snapColumnToCharBoundary(lineText, newStart);
        }
    }

    /**
     * markを設定する。
     */
    public void setMark(int mark) {
        int length = bufferFacade.length();
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
        int length = bufferFacade.length();
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
