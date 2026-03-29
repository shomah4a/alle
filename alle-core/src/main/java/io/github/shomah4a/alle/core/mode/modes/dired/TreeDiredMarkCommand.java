package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * カーソル行のエントリをマークし、次行に移動する。
 */
public class TreeDiredMarkCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-mark";
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

        diredMode.getModel().mark(entryOpt.get().path());
        TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
        return context.delegate("next-line");
    }
}
