package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを次の行に移動するコマンド。
 * Emacsのnext-lineに相当する。
 * 移動先の行が現在のカラム位置より短い場合は行末に移動する。
 */
public class NextLineCommand implements Command {

    @Override
    public String name() {
        return "next-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var actor = context.activeWindowActor();
        return actor.atomicPerform(window -> {
            var buffer = window.getBuffer();
            int point = window.getPoint();
            int currentLine = buffer.lineIndexForOffset(point);
            int lineCount = buffer.lineCount();

            if (currentLine >= lineCount - 1) {
                return null;
            }

            int currentLineStart = buffer.lineStartOffset(currentLine);
            int column = point - currentLineStart;

            int nextLineStart = buffer.lineStartOffset(currentLine + 1);
            int nextLineLength =
                    (int) buffer.lineText(currentLine + 1).codePoints().count();
            int newColumn = Math.min(column, nextLineLength);

            window.setPoint(nextLineStart + newColumn);
            return null;
        });
    }
}
