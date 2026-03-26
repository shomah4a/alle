package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.input.ShutdownRequestable;
import java.util.concurrent.CompletableFuture;

/**
 * エディタのプロセスを即時終了するコマンド。
 * ShutdownRequestableのshutdownフラグを立てて、CommandLoopを自然に終了させる。
 */
public class ProcessQuitCommand implements Command {

    private final ShutdownRequestable shutdownRequestable;

    public ProcessQuitCommand(ShutdownRequestable shutdownRequestable) {
        this.shutdownRequestable = shutdownRequestable;
    }

    @Override
    public String name() {
        return "quit";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        shutdownRequestable.requestShutdown();
        return CompletableFuture.completedFuture(null);
    }
}
