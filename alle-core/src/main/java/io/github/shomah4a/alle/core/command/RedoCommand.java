package io.github.shomah4a.alle.core.command;

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
        return context.activeWindowActor().redo().thenApply(v -> null);
    }
}
