package io.github.shomah4a.alle.core.command;

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
        return context.activeWindowActor().deleteForward(1);
    }
}
