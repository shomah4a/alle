package io.github.shomah4a.alle.core.render;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.styling.StylingState;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Rect;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowLayout;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;

/**
 * Frameの状態からimmutableな描画スナップショットを生成するファクトリ。
 *
 * <p>ensurePointVisible等の副作用を伴うスクロール位置調整もここで行う。
 * ロジックスレッドから呼び出される想定。
 */
public final class RenderSnapshotFactory {

    private RenderSnapshotFactory() {}

    /**
     * Frameの状態からimmutableな描画スナップショットを作成する。
     *
     * <p>副作用: 各ウィンドウのdisplayStartLine/displayStartColumnを
     * カーソルが表示範囲内に収まるよう調整する。
     *
     * @param frame エディタのフレーム
     * @param messageBuffer メッセージバッファ（エコーエリア表示用）
     * @param cols 画面の列数
     * @param rows 画面の行数
     * @return 描画スナップショット
     */
    public static RenderSnapshot create(Frame frame, MessageBuffer messageBuffer, int cols, int rows) {
        int windowAreaRows = rows - 1;
        int minibufferRow = rows - 1;

        var treeArea = new Rect(0, 0, cols, windowAreaRows);
        var layoutResult = WindowLayout.compute(frame.getWindowTree(), treeArea);

        var windowSnapshots = Lists.mutable.<RenderSnapshot.WindowSnapshot>empty();

        layoutResult.windowRects().forEachKeyValue((window, rect) -> {
            if (rect.height() < 2) {
                return;
            }
            int bufferRows = rect.height() - 1;
            window.ensurePointVisible(bufferRows);
            window.ensurePointHorizontallyVisible(rect.width());

            var buffer = window.getBuffer();
            int lineCount = buffer.lineCount();
            int displayStart = window.getDisplayStartLine();
            int displayStartColumn = window.getDisplayStartColumn();
            var stylerOpt = buffer.getMajorMode().styler();

            var visibleLines = Lists.mutable.<RenderSnapshot.LineSnapshot>empty();
            if (stylerOpt.isPresent()) {
                SyntaxStyler styler = stylerOpt.get();
                StylingState styleState = styler.initialState();
                for (int i = 0; i < displayStart && i < lineCount; i++) {
                    styleState = styler.styleLineWithState(buffer.lineText(i), styleState)
                            .nextState();
                }
                for (int row = 0; row < bufferRows; row++) {
                    int lineIndex = displayStart + row;
                    if (lineIndex < lineCount) {
                        String lineText = buffer.lineText(lineIndex);
                        var result = styler.styleLineWithState(lineText, styleState);
                        visibleLines.add(new RenderSnapshot.LineSnapshot(lineText, Optional.of(result.spans())));
                        styleState = result.nextState();
                    }
                }
            } else {
                for (int row = 0; row < bufferRows; row++) {
                    int lineIndex = displayStart + row;
                    if (lineIndex < lineCount) {
                        String lineText = buffer.lineText(lineIndex);
                        visibleLines.add(new RenderSnapshot.LineSnapshot(lineText, Optional.empty()));
                    }
                }
            }

            String modeLine = buildModeLineText(window);
            windowSnapshots.add(new RenderSnapshot.WindowSnapshot(rect, visibleLines, displayStartColumn, modeLine));
        });

        // ミニバッファ / エコーエリア
        RenderSnapshot.MinibufferSnapshot minibufferSnapshot;
        if (frame.isMinibufferActive()) {
            var minibufferWindow = frame.getMinibufferWindow();
            minibufferWindow.ensurePointHorizontallyVisible(cols);
            var buffer = minibufferWindow.getBuffer();
            String text = buffer.length() > 0 ? buffer.getText() : "";
            minibufferSnapshot =
                    new RenderSnapshot.MinibufferSnapshot(Optional.of(text), minibufferWindow.getDisplayStartColumn());
        } else if (messageBuffer.isShowingMessage()) {
            var msg = messageBuffer.getLastMessage();
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(msg, 0);
        } else {
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(Optional.empty(), 0);
        }

        // カーソル位置
        CursorPosition cursorPosition;
        if (frame.isMinibufferActive()) {
            cursorPosition = computeMinibufferCursorPosition(frame.getMinibufferWindow(), minibufferRow, cols);
        } else {
            var activeWindow = frame.getActiveWindow();
            var activeRect = layoutResult.windowRects().get(activeWindow);
            if (activeRect != null && activeRect.height() >= 2) {
                cursorPosition = computeCursorPosition(activeWindow, activeRect);
            } else {
                cursorPosition = new CursorPosition(0, 0);
            }
        }

        return new RenderSnapshot(
                cols, rows, windowSnapshots, layoutResult.separators(), minibufferSnapshot, cursorPosition);
    }

    private static String buildModeLineText(Window window) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int column = point - lineStart;

        String dirty = buffer.isDirty() ? "**" : "--";
        String bufferName = buffer.getName();
        String modeName = buffer.getMajorMode().name();
        var minorModes = buffer.getMinorModes();
        String minorModesText = minorModes.isEmpty()
                ? ""
                : " " + minorModes.collect(m -> m.name()).makeString(" ");
        return String.format(
                "--%s  %s    (%d,%d)  (%s%s)", dirty, bufferName, lineIndex + 1, column, modeName, minorModesText);
    }

    private static CursorPosition computeCursorPosition(Window window, Rect rect) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int displayStart = window.getDisplayStartLine();
        int bufferRows = rect.height() - 1;

        int screenRow = lineIndex - displayStart;
        if (screenRow < 0 || screenRow >= bufferRows) {
            return new CursorPosition(rect.left(), rect.top());
        }

        int col = DisplayWidthUtil.computeColumnForOffset(buffer.lineText(lineIndex), point - lineStart);
        int screenCol = col - window.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < rect.width()) {
            return new CursorPosition(rect.left() + screenCol, rect.top() + screenRow);
        }
        return new CursorPosition(rect.left(), rect.top());
    }

    private static CursorPosition computeMinibufferCursorPosition(Window minibufferWindow, int row, int maxColumns) {
        var buffer = minibufferWindow.getBuffer();
        int point = minibufferWindow.getPoint();
        String text = buffer.getText();
        int col = DisplayWidthUtil.computeColumnForOffset(text, point);
        int screenCol = col - minibufferWindow.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < maxColumns) {
            return new CursorPosition(screenCol, row);
        }
        return new CursorPosition(0, row);
    }
}
