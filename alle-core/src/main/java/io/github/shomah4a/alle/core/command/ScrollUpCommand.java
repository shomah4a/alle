package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * ページ下方向にスクロールするコマンド。
 * Emacsのscroll-up (C-v) に相当する。
 * ウィンドウの表示行数からオーバーラップ2行を引いた分だけ下に移動する。
 */
public class ScrollUpCommand implements Command {

    private static final int OVERLAP_LINES = 2;

    @Override
    public String name() {
        return "scroll-up";
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
        int lineCount = buffer.lineCount();

        int newDisplayStart = Math.min(window.getDisplayStartLine() + scrollAmount, lineCount - 1);
        window.setDisplayStartLine(newDisplayStart);

        int targetLine = Math.min(currentLine + scrollAmount, lineCount - 1);
        int targetLineStart = buffer.lineStartOffset(targetLine);
        int targetLineLength = (int) buffer.lineText(targetLine).codePoints().count();
        int newColumn = Math.min(column, targetLineLength);
        window.setPoint(targetLineStart + newColumn);

        return CompletableFuture.completedFuture(null);
    }
}
