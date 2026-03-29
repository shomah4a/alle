package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを前の行に移動するコマンド。
 * Emacsのprevious-lineに相当する。
 * 移動先の行が現在のカラム位置より短い場合は行末に移動する。
 */
public class PreviousLineCommand implements Command {

    @Override
    public String name() {
        return "previous-line";
    }

    @Override
    public boolean keepsRegionActive() {
        return true;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int currentLine = buffer.lineIndexForOffset(point);

        if (currentLine <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        int currentLineStart = buffer.lineStartOffset(currentLine);
        int column = point - currentLineStart;

        int prevLineStart = buffer.lineStartOffset(currentLine - 1);
        int prevLineLength = (int) buffer.lineText(currentLine - 1).codePoints().count();
        int newColumn = Math.min(column, prevLineLength);

        window.setPoint(prevLineStart + newColumn);
        return CompletableFuture.completedFuture(null);
    }
}
