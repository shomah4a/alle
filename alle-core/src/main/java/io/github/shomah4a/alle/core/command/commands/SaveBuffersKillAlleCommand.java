package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.ShutdownHandler;
import io.github.shomah4a.alle.core.input.ShutdownRequestable;
import java.util.concurrent.CompletableFuture;

/**
 * 終了処理ハンドラを実行し、全て通過したらエディタを終了するコマンド。
 * C-x C-c にバインドされる。
 */
public class SaveBuffersKillAlleCommand implements Command {

    private final ShutdownHandler shutdownHandler;
    private final ShutdownRequestable shutdownRequestable;

    public SaveBuffersKillAlleCommand(ShutdownHandler shutdownHandler, ShutdownRequestable shutdownRequestable) {
        this.shutdownHandler = shutdownHandler;
        this.shutdownRequestable = shutdownRequestable;
    }

    @Override
    public String name() {
        return "save-buffers-kill-alle";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        shutdownHandler
                .executeAll()
                .thenAccept(shouldQuit -> {
                    if (shouldQuit) {
                        shutdownRequestable.requestShutdown();
                    }
                })
                .exceptionally(ex -> {
                    context.handleError("終了処理中にエラーが発生", ex);
                    return null;
                });
        return CompletableFuture.completedFuture(null);
    }
}
