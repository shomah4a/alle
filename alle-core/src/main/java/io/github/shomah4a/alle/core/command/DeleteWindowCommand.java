package io.github.shomah4a.alle.core.command;

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
        return context.frameActor().isMinibufferActive().thenCompose(active -> {
            if (active) {
                return CompletableFuture.completedFuture(null);
            }
            return context.frameActor().deleteActiveWindow().thenApply(v -> null);
        });
    }
}
