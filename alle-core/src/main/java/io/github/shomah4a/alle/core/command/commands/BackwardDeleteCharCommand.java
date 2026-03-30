package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * カーソル前の文字を削除するコマンド。
 * Emacsのbackward-delete-charに相当する。
 */
public class BackwardDeleteCharCommand implements Command {

    @Override
    public String name() {
        return "backward-delete-char";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        context.activeWindow().deleteBackward(1);
        return CompletableFuture.completedFuture(null);
    }
}
