package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * カーソル位置に改行を挿入するコマンド。
 * Emacsのnewlineに相当する。
 */
public class NewlineCommand implements Command {

    @Override
    public String name() {
        return "newline";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        context.activeWindow().insert("\n");
        return CompletableFuture.completedFuture(null);
    }
}
