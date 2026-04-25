package io.github.shomah4a.alle.core.server;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * server-start コマンド。
 * Emacs の server-start に相当する。
 * サーバーソケットを開始し、クライアントからの接続を受け付ける。
 */
public class ServerStartCommand implements Command, Loggable {

    private final ServerStarter serverStarter;

    /**
     * サーバーを起動する処理を表すインターフェース。
     * Main 側で ServerManager.start の呼び出しを組み立てて渡す。
     */
    public interface ServerStarter {
        void start() throws IOException;
    }

    public ServerStartCommand(ServerStarter serverStarter) {
        this.serverStarter = serverStarter;
    }

    @Override
    public String name() {
        return "server-start";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        try {
            serverStarter.start();
            context.messageBuffer().message("Server started: " + ServerManager.resolveSocketPath());
        } catch (IOException e) {
            var message = "Server start failed: " + e.getMessage();
            logger().warn(message, e);
            context.messageBuffer().message(message);
        }
        return CompletableFuture.completedFuture(null);
    }
}
