package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
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
    public boolean keepsRegionActive() {
        return true;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        window.setMark(window.getPoint());
        return CompletableFuture.completedFuture(null);
    }
}
