package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * 現在のカーソル位置にmarkを設定するコマンド。
 * Emacsのset-mark-command (C-SPC) に相当する。
 */
public class SetMarkCommand implements Command {

    @Override
    public String name() {
        return "set-mark";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var actor = context.activeWindowActor();
        return actor.getPoint().thenCompose(actor::setMark);
    }
}
