package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * カーソル行がファイルなら開き、ディレクトリなら展開/折り畳みを行う。
 */
public class TreeDiredFindFileOrToggleCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-find-file-or-toggle";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        var entryOpt = TreeDiredEntryResolver.resolve(context.activeWindow(), diredMode);
        if (entryOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var entry = entryOpt.get();
        if (entry.isDirectory()) {
            diredMode.getModel().toggle(entry.path());
            TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
            return CompletableFuture.completedFuture(null);
        }

        context.openPath(entry.path().toString());
        return CompletableFuture.completedFuture(null);
    }
}
