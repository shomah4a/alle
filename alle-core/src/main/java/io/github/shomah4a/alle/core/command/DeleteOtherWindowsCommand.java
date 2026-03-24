package io.github.shomah4a.alle.core.command;

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
        return context.frameActor().isMinibufferActive().thenCompose(active -> {
            if (active) {
                return CompletableFuture.completedFuture(null);
            }
            return context.frameActor().deleteOtherWindows();
        });
    }
}
