package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.VisualLineUtil;
import java.util.concurrent.CompletableFuture;

/**
 * ページ下方向にスクロールするコマンド。
 * Emacsのscroll-up (C-v) に相当する。
 * ウィンドウの表示行数からオーバーラップ2行を引いた分だけ下に移動する。
 * 折り返しモード時は視覚行単位でスクロールする。
 */
public class ScrollUpCommand implements Command {

    private static final int OVERLAP_LINES = 2;

    @Override
    public String name() {
        return "scroll-up";
    }

    @Override
    public boolean keepsRegionActive() {
        return true;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        int visibleRows = window.getViewportSize().rows();
        if (visibleRows <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        int scrollAmount = Math.max(1, visibleRows - OVERLAP_LINES);

        if (window.isTruncateLines()) {
            int point = window.getPoint();
            int currentLine = buffer.lineIndexForOffset(point);
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int column = point - currentLineStart;
            int lineCount = buffer.lineCount();

            int newDisplayStart = Math.min(window.getDisplayStartLine() + scrollAmount, lineCount - 1);
            window.setDisplayStartLine(newDisplayStart);

            int targetLine = Math.min(currentLine + scrollAmount, lineCount - 1);
            int targetLineStart = buffer.lineStartOffset(targetLine);
            int targetLineLength =
                    (int) buffer.lineText(targetLine).codePoints().count();
            int newColumn = Math.min(column, targetLineLength);
            window.setPoint(targetLineStart + newColumn);
        } else {
            int columns = window.getViewportSize().columns();
            int lineCount = buffer.lineCount();

            // displayStartLineを視覚行単位で進める
            int remaining = scrollAmount;
            int newDisplayStart = window.getDisplayStartLine();
            while (remaining > 0 && newDisplayStart < lineCount - 1) {
                String lineText = buffer.lineText(newDisplayStart);
                int vlCount = VisualLineUtil.computeVisualLineCount(lineText, columns);
                if (vlCount <= remaining) {
                    remaining -= vlCount;
                    newDisplayStart++;
                } else {
                    break;
                }
            }
            window.setDisplayStartLine(newDisplayStart);

            // カーソルも視覚行単位で進める
            int point = window.getPoint();
            int currentLine = buffer.lineIndexForOffset(point);
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int cpOffset = point - currentLineStart;
            String currentLineText = buffer.lineText(currentLine);
            int currentVisualLine = VisualLineUtil.computeVisualLineForOffset(currentLineText, columns, cpOffset);
            int currentLineVisualCount = VisualLineUtil.computeVisualLineCount(currentLineText, columns);

            int visualRemaining = scrollAmount;
            int targetBufferLine = currentLine;
            int targetVisualLine = currentVisualLine;

            // 現在行の残り視覚行を消費
            int remainingInCurrentLine = currentLineVisualCount - currentVisualLine - 1;
            if (visualRemaining <= remainingInCurrentLine) {
                targetVisualLine = currentVisualLine + visualRemaining;
                visualRemaining = 0;
            } else {
                visualRemaining -= remainingInCurrentLine + 1;
                targetBufferLine++;
            }

            // 次の行以降を消費
            while (visualRemaining > 0 && targetBufferLine < lineCount) {
                String lineText = buffer.lineText(targetBufferLine);
                int vlCount = VisualLineUtil.computeVisualLineCount(lineText, columns);
                if (visualRemaining < vlCount) {
                    targetVisualLine = visualRemaining;
                    visualRemaining = 0;
                } else {
                    visualRemaining -= vlCount;
                    targetBufferLine++;
                    targetVisualLine = 0;
                }
            }

            if (targetBufferLine >= lineCount) {
                targetBufferLine = lineCount - 1;
                targetVisualLine = 0;
            }

            int targetLineStart = buffer.lineStartOffset(targetBufferLine);
            String targetLineText = buffer.lineText(targetBufferLine);
            int vlStartCp = VisualLineUtil.visualLineStartOffset(targetLineText, columns, targetVisualLine);
            int targetLineLength = (int) targetLineText.codePoints().count();
            int newCp = Math.min(vlStartCp, targetLineLength);
            window.setPoint(targetLineStart + newCp);
        }

        return CompletableFuture.completedFuture(null);
    }
}
