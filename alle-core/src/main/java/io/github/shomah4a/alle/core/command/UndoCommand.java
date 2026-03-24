package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * 直前のテキスト変更を取り消すコマンド。
 * Emacsのundo (C-/) に相当する。
 */
public class UndoCommand implements Command {

    @Override
    public String name() {
        return "undo";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().undo().thenApply(v -> null);
    }
}
