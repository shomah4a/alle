package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * カーソルを1文字前方に移動するコマンド。
 * Emacsのforward-charに相当する。
 */
public class ForwardCharCommand implements Command {

    @Override
    public String name() {
        return "forward-char";
    }

    @Override
    public boolean keepsRegionActive() {
        return true;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        int point = window.getPoint();
        int length = window.getBuffer().length();
        if (point < length) {
            window.setPoint(point + 1);
        }
        return CompletableFuture.completedFuture(null);
    }
}
