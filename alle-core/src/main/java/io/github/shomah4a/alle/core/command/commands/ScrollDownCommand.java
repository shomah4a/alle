package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.VisualLineUtil;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * ページ上方向にスクロールするコマンド。
 * Emacsのscroll-down (M-v) に相当する。
 * ウィンドウの表示行数からオーバーラップ2行を引いた分だけ上に移動する。
 * 折り返しモード時は視覚行単位でスクロールする。
 */
public class ScrollDownCommand implements Command {

    private static final int OVERLAP_LINES = 2;

    @Override
    public String name() {
        return "scroll-down";
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

            int newDisplayStart = Math.max(0, window.getDisplayStartLine() - scrollAmount);
            window.setDisplayStartLine(newDisplayStart);

            int targetLine = Math.max(0, currentLine - scrollAmount);
            int targetLineStart = buffer.lineStartOffset(targetLine);
            int targetLineLength =
                    (int) buffer.lineText(targetLine).codePoints().count();
            int newColumn = Math.min(column, targetLineLength);
            window.setPoint(targetLineStart + newColumn);
        } else {
            int columns = window.getViewportSize().columns();
            int tabWidth = buffer.getSettings().get(EditorSettings.TAB_WIDTH);

            // displayStartLineを視覚行単位で戻す
            int remaining = scrollAmount;
            int newDisplayStart = window.getDisplayStartLine();
            while (remaining > 0 && newDisplayStart > 0) {
                newDisplayStart--;
                String lineText = buffer.lineText(newDisplayStart);
                int vlCount = VisualLineUtil.computeVisualLineCount(lineText, columns, tabWidth);
                remaining -= vlCount;
            }
            if (newDisplayStart < 0) {
                newDisplayStart = 0;
            }
            window.setDisplayStartLine(newDisplayStart);

            // カーソルも視覚行単位で戻す
            int point = window.getPoint();
            int currentLine = buffer.lineIndexForOffset(point);
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int cpOffset = point - currentLineStart;
            String currentLineText = buffer.lineText(currentLine);
            int currentVisualLine =
                    VisualLineUtil.computeVisualLineForOffset(currentLineText, columns, cpOffset, tabWidth);

            int visualRemaining = scrollAmount;
            int targetBufferLine = currentLine;
            int targetVisualLine = currentVisualLine;

            // 現在行の前方の視覚行を消費
            if (visualRemaining <= currentVisualLine) {
                targetVisualLine = currentVisualLine - visualRemaining;
                visualRemaining = 0;
            } else {
                visualRemaining -= currentVisualLine + 1;
                targetBufferLine--;
            }

            // 前の行を消費
            while (visualRemaining > 0 && targetBufferLine >= 0) {
                String lineText = buffer.lineText(targetBufferLine);
                int vlCount = VisualLineUtil.computeVisualLineCount(lineText, columns, tabWidth);
                if (visualRemaining < vlCount) {
                    targetVisualLine = vlCount - 1 - visualRemaining;
                    visualRemaining = 0;
                } else {
                    visualRemaining -= vlCount;
                    targetBufferLine--;
                    targetVisualLine = 0;
                }
            }

            if (targetBufferLine < 0) {
                targetBufferLine = 0;
                targetVisualLine = 0;
            }

            int targetLineStart = buffer.lineStartOffset(targetBufferLine);
            String targetLineText = buffer.lineText(targetBufferLine);
            int vlStartCp = VisualLineUtil.visualLineStartOffset(targetLineText, columns, targetVisualLine, tabWidth);
            int targetLineLength = (int) targetLineText.codePoints().count();
            int newCp = Math.min(vlStartCp, targetLineLength);
            window.setPoint(targetLineStart + newCp);
        }

        return CompletableFuture.completedFuture(null);
    }
}
