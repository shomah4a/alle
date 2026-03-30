package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * occurウィンドウを閉じるコマンド。
 */
public class OccurQuitCommand implements Command {

    @Override
    public String name() {
        return "occur-quit";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        context.frame().deleteWindow(context.activeWindow());
        return CompletableFuture.completedFuture(null);
    }
}
