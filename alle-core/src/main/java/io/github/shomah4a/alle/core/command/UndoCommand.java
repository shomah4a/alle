package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * 直前のテキスト変更を取り消すコマンド。
 * Emacsのundo (C-/) に相当する。
 */
public class UndoCommand implements Command {

    @Override
    public String name() {
        return "undo";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        var undoManager = buffer.getUndoManager();
        var entryOpt = undoManager.undo();
        if (entryOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        var entry = entryOpt.get();
        undoManager.suppressRecording();
        try {
            buffer.apply(entry.change());
            window.setPoint(entry.cursorPosition());
        } finally {
            undoManager.resumeRecording();
        }
        return CompletableFuture.completedFuture(null);
    }
}
