package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * カーソル行のディレクトリを展開/折り畳みする。
 * ファイル行やヘッダ行では何もしない。
 */
public class TreeDiredToggleCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-toggle";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        var entryOpt = TreeDiredEntryResolver.resolve(context.activeWindow(), diredMode);
        if (entryOpt.isEmpty() || !entryOpt.get().isDirectory()) {
            return CompletableFuture.completedFuture(null);
        }

        diredMode.getModel().toggle(entryOpt.get().path());
        TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
        return CompletableFuture.completedFuture(null);
    }
}
