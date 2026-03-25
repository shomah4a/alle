package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.TextChange;
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
        var entryOpt = undoManager.redo();
        if (entryOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        var entry = entryOpt.get();
        undoManager.suppressRecording();
        try {
            buffer.apply(entry.change());
            // redo後のカーソル位置は操作適用後の位置
            var change = entry.change();
            if (change instanceof TextChange.Insert insert) {
                int insertedLen = (int) insert.text().codePoints().count();
                window.setPoint(insert.offset() + insertedLen);
            } else if (change instanceof TextChange.Delete delete) {
                window.setPoint(delete.offset());
            }
        } finally {
            undoManager.resumeRecording();
        }
        return CompletableFuture.completedFuture(null);
    }
}
