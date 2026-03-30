package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * 行の切り詰め/折り返しモードを切り替えるコマンド。
 * Emacsのtoggle-truncate-linesに相当する。
 */
public class ToggleTruncateLinesCommand implements Command {

    @Override
    public String name() {
        return "toggle-truncate-lines";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        boolean newValue = !window.isTruncateLines();
        window.setTruncateLines(newValue);
        String modeText = newValue ? "Truncate" : "Wrap";
        context.messageBuffer().message(modeText + " long lines");
        return CompletableFuture.completedFuture(null);
    }
}
