package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * 展開状態を保持したままバッファ内容を再読み込みする。
 */
public class TreeDiredRefreshCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-refresh";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
        return CompletableFuture.completedFuture(null);
    }
}
