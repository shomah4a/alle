package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * カーソル位置の文字を削除するコマンド。
 * Emacsのdelete-charに相当する。
 */
public class DeleteCharCommand implements Command {

    @Override
    public String name() {
        return "delete-char";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        context.activeWindow().deleteForward(1);
        return CompletableFuture.completedFuture(null);
    }
}
