package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * シェルプロセスに SIGINT を送信するコマンド。
 * C-c C-c キーにバインドされる。
 */
final class ShellInterruptCommand implements Command {

    private static final int SIGINT = 2;

    @Override
    public String name() {
        return "shell-interrupt";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof ShellMode shellMode)) {
            return CompletableFuture.completedFuture(null);
        }
        shellMode.getModel().getProcess().sendSignal(SIGINT);
        return CompletableFuture.completedFuture(null);
    }
}
