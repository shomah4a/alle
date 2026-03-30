package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * カーソルを1文字後方に移動するコマンド。
 * Emacsのbackward-charに相当する。
 */
public class BackwardCharCommand implements Command {

    @Override
    public String name() {
        return "backward-char";
    }

    @Override
    public boolean keepsRegionActive() {
        return true;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        int point = window.getPoint();
        if (point > 0) {
            window.setPoint(point - 1);
        }
        return CompletableFuture.completedFuture(null);
    }
}
