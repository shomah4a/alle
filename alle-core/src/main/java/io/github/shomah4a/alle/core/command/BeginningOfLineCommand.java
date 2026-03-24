package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを行頭に移動するコマンド。
 * Emacsのbeginning-of-lineに相当する。
 */
public class BeginningOfLineCommand implements Command {

    @Override
    public String name() {
        return "beginning-of-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().moveToBeginningOfLine();
    }
}
