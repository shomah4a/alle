package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを行末に移動するコマンド。
 * Emacsのend-of-lineに相当する。
 */
public class EndOfLineCommand implements Command {

    @Override
    public String name() {
        return "end-of-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().moveToEndOfLine();
    }
}
