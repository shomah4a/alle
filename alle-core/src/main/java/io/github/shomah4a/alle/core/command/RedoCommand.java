package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * 直前のundoをやり直すコマンド。
 * C-? に相当する。
 */
public class RedoCommand implements Command {

    @Override
    public String name() {
        return "redo";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        var undoManager = buffer.getUndoManager();
        var changeOpt = undoManager.redo();
        if (changeOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        var change = changeOpt.get();
        buffer.apply(change);
        window.setPoint(change.cursorAfterApply());
        return CompletableFuture.completedFuture(null);
    }
}
