package io.github.shomah4a.alle.core.command;

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
        context.frame().getActiveWindow().insert("\n");
        return CompletableFuture.completedFuture(null);
    }
}
