package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * ページ上方向にスクロールするコマンド。
 * Emacsのscroll-down (M-v) に相当する。
 * ウィンドウの表示行数からオーバーラップ2行を引いた分だけ上に移動する。
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

        int point = window.getPoint();
        int currentLine = buffer.lineIndexForOffset(point);
        int currentLineStart = buffer.lineStartOffset(currentLine);
        int column = point - currentLineStart;

        int scrollAmount = Math.max(1, visibleRows - OVERLAP_LINES);

        int newDisplayStart = Math.max(0, window.getDisplayStartLine() - scrollAmount);
        window.setDisplayStartLine(newDisplayStart);

        int targetLine = Math.max(0, currentLine - scrollAmount);
        int targetLineStart = buffer.lineStartOffset(targetLine);
        int targetLineLength = (int) buffer.lineText(targetLine).codePoints().count();
        int newColumn = Math.min(column, targetLineLength);
        window.setPoint(targetLineStart + newColumn);

        return CompletableFuture.completedFuture(null);
    }
}
