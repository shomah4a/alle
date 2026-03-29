package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを行頭に移動するコマンド。
 * Emacsのbeginning-of-lineに相当する。
 */
public class BeginningOfLineCommand implements Command {

    @Override
    public String name() {
        return "beginning-of-line";
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
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        window.setPoint(lineStart);
        return CompletableFuture.completedFuture(null);
    }
}
