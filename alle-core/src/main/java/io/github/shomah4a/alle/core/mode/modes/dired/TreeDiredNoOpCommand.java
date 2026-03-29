package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * 何もしないコマンド。未バインドキーの self-insert 抑制用。
 */
public class TreeDiredNoOpCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-noop";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return CompletableFuture.completedFuture(null);
    }
}
