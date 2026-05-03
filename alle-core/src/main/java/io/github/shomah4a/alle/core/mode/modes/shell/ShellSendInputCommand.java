package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * シェルバッファのユーザー入力をプロセスの stdin に送信するコマンド。
 * RET キーにバインドされる。
 */
final class ShellSendInputCommand implements Command {

    @Override
    public String name() {
        return "shell-send-input";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof ShellMode shellMode)) {
            return CompletableFuture.completedFuture(null);
        }
        shellMode.getModel().sendInput();
        context.activeWindow().setPoint(context.activeWindow().getBuffer().length());
        return CompletableFuture.completedFuture(null);
    }
}
