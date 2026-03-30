package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * アクティブウィンドウを閉じるコマンド。
 * 最後の1つのウィンドウは閉じない。
 */
public class DeleteWindowCommand implements Command {

    @Override
    public String name() {
        return "delete-window";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var frame = context.frame();
        if (frame.isMinibufferActive()) {
            return CompletableFuture.completedFuture(null);
        }
        frame.deleteWindow(frame.getActiveWindow());
        return CompletableFuture.completedFuture(null);
    }
}
