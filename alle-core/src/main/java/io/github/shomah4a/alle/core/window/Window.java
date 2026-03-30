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
    private int displayStartVisualLine;
    private int displayStartColumn;
    private @Nullable Integer mark;
    private boolean highlightPointLine;
    private boolean truncateLines = false;
    private volatile ViewportSize viewportSize = new ViewportSize(0, 0);

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
        this.displayStartVisualLine = 0;
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
     * pointGuard範囲内にある場合はガード範囲のend位置にクランプする。
     */
    public int getPoint() {
        int length = bufferFacade.length();
        if (point > length) {
            point = length;
        }
        int resolved = bufferFacade.resolvePointGuard(point, true);
        if (resolved != point && resolved <= length) {
            point = resolved;
        }
        return point;
    }

    /**
     * カーソル位置(point)をコードポイント単位で設定する。
     * pointGuard範囲内の場合、侵入方向と逆方向に押し戻す。
     *
     * @throws IndexOutOfBoundsException pointが負の値またはバッファ長を超える場合
     */
    public void setPoint(int point) {
        int length = bufferFacade.length();
        if (point < 0 || point > length) {
            throw new IndexOutOfBoundsException("point " + point + " is out of bounds [0, " + length + "]");
        }
        boolean forward = point >= this.point;
        int resolved = bufferFacade.resolvePointGuard(point, forward);
        if (resolved >= 0 && resolved <= length) {
            this.point = resolved;
        } else {
            this.point = point;
        }
    }

    /**
     * 表示開始行を返す。
     */
    public int getDisplayStartLine() {
        return displayStartLine;
    }

    /**
     * 表示開始行を設定する。
     * 表示開始視覚行は0にリセットされる。
     *
     * @throws IndexOutOfBoundsException lineが範囲外の場合
     */
    public void setDisplayStartLine(int line) {
        int lineCount = bufferFacade.lineCount();
        if (line < 0 || line >= lineCount) {
            throw new IndexOutOfBoundsException("line " + line + " is out of bounds [0, " + lineCount + ")");
        }
        this.displayStartLine = line;
        this.displayStartVisualLine = 0;
    }

    /**
     * 折り返しモード時の表示開始視覚行（displayStartLine内の視覚行番号、0始まり）を返す。
     * 1バッファ行が画面行数を超える場合に、行の途中から表示を開始するために使用する。
     */
    public int getDisplayStartVisualLine() {
        return displayStartVisualLine;
    }

    /**
     * 折り返しモード時の表示開始視覚行を設定する。
     */
    public void setDisplayStartVisualLine(int visualLine) {
        this.displayStartVisualLine = visualLine;
    }

    /**
     * カーソル位置に文字列を挿入し、カーソルを挿入文字列の後ろに移動する。
     */
    public void insert(String text) {
        bufferFacade.insertText(point, text);
        int insertedCodePoints = (int) text.codePoints().count();
        point += insertedCodePoints;
        bufferFacade.markDirty();
    }

    /**
     * カーソルの手前からcount文字を削除する(バックスペース相当)。
     * pointGuard範囲に食い込む場合、ガード範囲のend位置まで削除範囲を制限する。
     */
    public void deleteBackward(int count) {
        int effectivePoint = getPoint();
        int rawStart = effectivePoint - Math.min(count, effectivePoint);
        int resolvedStart = bufferFacade.resolvePointGuard(rawStart, true);
        int deleteCount = effectivePoint - resolvedStart;
        if (deleteCount <= 0) {
            return;
        }
        bufferFacade.deleteText(resolvedStart, deleteCount);
        point = resolvedStart;
        bufferFacade.markDirty();
    }

    /**
     * カーソルの後ろからcount文字を削除する(Delete相当)。
     * 削除範囲内にpointGuard範囲がある場合、最初のガード位置の手前まで制限する。
     */
    public void deleteForward(int count) {
        int effectivePoint = getPoint();
        int remaining = bufferFacade.length() - effectivePoint;
        int deleteCount = Math.min(count, remaining);
        for (int i = 0; i < deleteCount; i++) {
            if (bufferFacade.isPointGuardAt(effectivePoint + i)) {
                deleteCount = i;
                break;
            }
        }
        if (deleteCount == 0) {
            return;
        }
        bufferFacade.deleteText(effectivePoint, deleteCount);
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
        if (!truncateLines) {
            displayStartColumn = 0;
            return;
        }
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

    /**
     * ポイント行ハイライトが有効かどうかを返す。
     * 有効な場合、ポイントが存在する行を反転表示でハイライトする。
     */
    public boolean isHighlightPointLine() {
        return highlightPointLine;
    }

    /**
     * ポイント行ハイライトの有効/無効を設定する。
     */
    public void setHighlightPointLine(boolean highlightPointLine) {
        this.highlightPointLine = highlightPointLine;
    }

    /**
     * 行切り詰めモードかどうかを返す。
     * trueの場合、ウィンドウ幅を超える行は切り詰めて水平スクロールで表示する。
     * falseの場合、ウィンドウ幅で折り返して表示する。
     */
    public boolean isTruncateLines() {
        return truncateLines;
    }

    /**
     * 行切り詰めモードを設定する。
     * falseに設定すると折り返しモードになり、水平スクロール位置は0にリセットされる。
     */
    public void setTruncateLines(boolean truncateLines) {
        this.truncateLines = truncateLines;
        if (!truncateLines) {
            this.displayStartColumn = 0;
        }
    }

    /**
     * 表示可能領域のサイズを返す。
     * レンダリング前はrows=0, columns=0を返す。
     */
    public ViewportSize getViewportSize() {
        return viewportSize;
    }

    /**
     * 表示可能領域のサイズを設定する。
     * レンダリング時にレイアウト計算結果から呼び出される。
     */
    public void setViewportSize(ViewportSize viewportSize) {
        this.viewportSize = viewportSize;
    }
}
