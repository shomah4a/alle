package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * シェルプロセスに SIGTSTP を送信するコマンド。
 * C-c C-z キーにバインドされる。
 */
final class ShellSuspendCommand implements Command {

    private static final int SIGTSTP = 20;

    @Override
    public String name() {
        return "shell-suspend";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof ShellMode shellMode)) {
            return CompletableFuture.completedFuture(null);
        }
        shellMode.getModel().getProcess().sendSignal(SIGTSTP);
        return CompletableFuture.completedFuture(null);
    }
}
