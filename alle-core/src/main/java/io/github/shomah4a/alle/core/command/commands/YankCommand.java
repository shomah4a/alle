package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * kill-ringの最新エントリをカーソル位置に挿入するコマンド。
 * Emacsのyank (C-y) に相当する。
 * kill-ringが空の場合は何もしない。
 */
public class YankCommand implements Command {

    @Override
    public String name() {
        return "yank";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var textOpt = context.killRing().current();
        if (textOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        window.insert(textOpt.get());
        return CompletableFuture.completedFuture(null);
    }
}
