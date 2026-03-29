package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * ルートディレクトリの親ディレクトリに移動する。
 * ルートが "/" の場合は何もしない。
 */
public class TreeDiredUpDirectoryCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-up-directory";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        Path currentRoot = diredMode.getModel().getRootDirectory();
        Path parent = currentRoot.getParent();
        if (parent == null) {
            return CompletableFuture.completedFuture(null);
        }

        diredMode.getModel().setRootDirectory(parent);
        TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
        context.activeWindow().setPoint(0);
        return CompletableFuture.completedFuture(null);
    }
}
