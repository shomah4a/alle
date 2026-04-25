package io.github.shomah4a.alle.core.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 1つのクライアント接続を表すセッション。
 * クライアントへのレスポンス送信と、編集完了通知を管理する。
 */
public class ServerSession {

    private static final Logger logger = Logger.getLogger(ServerSession.class.getName());

    private final SocketChannel channel;
    private final Path filePath;
    private final CompletableFuture<Void> completion;

    public ServerSession(SocketChannel channel, Path filePath) {
        this.channel = channel;
        this.filePath = filePath;
        this.completion = new CompletableFuture<>();
    }

    public Path filePath() {
        return filePath;
    }

    /**
     * 編集完了をクライアントに通知する。
     * ロジックスレッドから呼ばれる。FINISHED を送信し、チャネルをクローズする。
     */
    public void notifyFinished() {
        sendResponse(new ServerProtocol.Response.Finished());
        closeChannel();
        completion.complete(null);
    }

    /**
     * エラーをクライアントに通知する。
     */
    public void notifyError(String message) {
        sendResponse(new ServerProtocol.Response.ServerError(message));
        closeChannel();
        completion.completeExceptionally(new IOException(message));
    }

    /**
     * ファイルオープン成功をクライアントに通知する。
     */
    public void sendOpened(String bufferName) {
        sendResponse(new ServerProtocol.Response.Opened(bufferName));
    }

    /**
     * クライアント側がブロックするための Future を返す。
     * セッション完了時に complete される。
     */
    public CompletableFuture<Void> completion() {
        return completion;
    }

    private void sendResponse(ServerProtocol.Response response) {
        var line = ServerProtocol.encode(response) + "\n";
        var buffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "クライアントへのレスポンス送信に失敗: " + line.trim(), e);
        }
    }

    private void closeChannel() {
        try {
            channel.close();
        } catch (IOException e) {
            logger.log(Level.FINE, "チャネルのクローズに失敗", e);
        }
    }
}
