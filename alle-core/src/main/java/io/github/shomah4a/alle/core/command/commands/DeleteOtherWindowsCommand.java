package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * アクティブウィンドウ以外のすべてのウィンドウを閉じるコマンド。
 */
public class DeleteOtherWindowsCommand implements Command {

    @Override
    public String name() {
        return "delete-other-windows";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var frame = context.frame();
        if (frame.isMinibufferActive()) {
            return CompletableFuture.completedFuture(null);
        }
        frame.deleteOtherWindows();
        return CompletableFuture.completedFuture(null);
    }
}
