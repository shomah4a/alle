package io.github.shomah4a.alle.core.server;

import io.github.shomah4a.alle.core.buffer.BufferKiller;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.io.BufferIO;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * server-edit コマンド (C-x #)。
 * Emacs の server-edit に相当する。
 * バッファを保存し、クライアントに完了を通知し、server マイナーモードを無効化してバッファを kill する。
 */
public class ServerEditCommand implements Command {

    private static final Logger logger = Logger.getLogger(ServerEditCommand.class.getName());

    private final BufferIO bufferIO;
    private final ServerSessionLookup sessionLookup;

    public ServerEditCommand(BufferIO bufferIO, ServerSessionLookup sessionLookup) {
        this.bufferIO = bufferIO;
        this.sessionLookup = sessionLookup;
    }

    @Override
    public String name() {
        return "server-edit";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var buffer = context.frame().getActiveWindow().getBuffer();

        // server マイナーモードが有効でなければ何もしない
        var serverModeOpt = buffer.getMinorModes().detect(m -> "server".equals(m.name()));
        if (serverModeOpt == null) {
            context.messageBuffer().message("No server editing buffers exist");
            return CompletableFuture.completedFuture(null);
        }

        // バッファを保存
        if (buffer.getFilePath().isPresent()) {
            try {
                bufferIO.save(buffer);
            } catch (IOException e) {
                var message = "バッファの保存に失敗: " + buffer.getName();
                logger.log(Level.WARNING, message, e);
                context.handleError(message, e);
                return CompletableFuture.completedFuture(null);
            }
        }

        // セッションに完了通知
        var filePath = buffer.getFilePath();
        if (filePath.isPresent()) {
            var sessionOpt = sessionLookup.findByPath(filePath.get());
            if (sessionOpt.isPresent()) {
                sessionOpt.get().notifyFinished();
                sessionLookup.removeByPath(filePath.get());
            }
        }

        // server マイナーモードを無効化
        buffer.disableMinorMode(serverModeOpt);

        // バッファを kill (バッファが1つしかない場合は kill しない)
        var bufferManager = context.bufferManager();
        if (bufferManager.size() > 1) {
            BufferKiller.kill(bufferManager, context.frame(), buffer.getName(), buffer, context.settingsRegistry());
        }

        return CompletableFuture.completedFuture(null);
    }
}
