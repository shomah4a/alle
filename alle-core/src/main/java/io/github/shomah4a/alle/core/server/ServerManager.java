package io.github.shomah4a.alle.core.server;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.io.PathOpenService;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.window.Frame;
import java.io.Closeable;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jspecify.annotations.Nullable;

/**
 * サーバーソケットのライフサイクルとクライアントセッションを管理する。
 * accept スレッドで接続を受け付け、アクションキュー経由でロジックスレッドに
 * ファイルオープンとマイナーモード付与を依頼する。
 */
public class ServerManager implements Closeable, ServerSessionLookup {

    private static final Logger logger = Logger.getLogger(ServerManager.class.getName());

    private final MutableMap<Path, ServerSession> sessions = Maps.mutable.empty();
    private volatile @Nullable ServerSocketChannel serverChannel;
    private volatile @Nullable Thread acceptThread;
    private volatile @Nullable Path socketPath;

    /**
     * サーバーソケットパスを解決する。
     */
    public static Path resolveSocketPath() {
        var xdgRuntime = System.getenv("XDG_RUNTIME_DIR");
        if (xdgRuntime != null && !xdgRuntime.isEmpty()) {
            return Path.of(xdgRuntime, "alle", "server");
        }
        return Path.of("/tmp", "alle-server-" + System.getProperty("user.name"));
    }

    /**
     * サーバーを起動する。
     *
     * @param actionQueue ロジックスレッドへのアクションキュー
     * @param pathOpenService ファイルオープンサービス
     * @param bufferManager バッファマネージャ
     * @param frame フレーム
     * @param serverMinorModeFactory server マイナーモード生成ファクトリ
     */
    public void start(
            BlockingQueue<Runnable> actionQueue,
            PathOpenService pathOpenService,
            BufferManager bufferManager,
            Frame frame,
            java.util.function.Supplier<MinorMode> serverMinorModeFactory)
            throws IOException {
        socketPath = resolveSocketPath();

        // 親ディレクトリを作成
        var parentDir = socketPath.getParent();
        if (parentDir != null) {
            Files.createDirectories(parentDir);
        }

        // stale socket detection
        cleanupStaleSocket(socketPath);

        var address = UnixDomainSocketAddress.of(socketPath);
        serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(address);

        acceptThread = new Thread(
                () -> acceptLoop(actionQueue, pathOpenService, bufferManager, frame, serverMinorModeFactory),
                "server-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        logger.info("サーバーを起動しました: " + socketPath);
    }

    private void acceptLoop(
            BlockingQueue<Runnable> actionQueue,
            PathOpenService pathOpenService,
            BufferManager bufferManager,
            Frame frame,
            java.util.function.Supplier<MinorMode> serverMinorModeFactory) {
        var channel = serverChannel;
        if (channel == null) {
            return;
        }
        while (!Thread.currentThread().isInterrupted()) {
            try {
                var clientChannel = channel.accept();
                handleClient(clientChannel, actionQueue, pathOpenService, bufferManager, frame, serverMinorModeFactory);
            } catch (IOException e) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                logger.log(Level.WARNING, "クライアント接続の受け付けに失敗", e);
            }
        }
    }

    private void handleClient(
            SocketChannel clientChannel,
            BlockingQueue<Runnable> actionQueue,
            PathOpenService pathOpenService,
            BufferManager bufferManager,
            Frame frame,
            java.util.function.Supplier<MinorMode> serverMinorModeFactory) {
        try {
            var requestLine = readLine(clientChannel);
            if (requestLine == null) {
                clientChannel.close();
                return;
            }

            var requestOpt = ServerProtocol.parseRequest(requestLine);
            if (requestOpt.isEmpty()) {
                sendResponse(clientChannel, new ServerProtocol.Response.ServerError("invalid request"));
                clientChannel.close();
                return;
            }

            var request = requestOpt.get();
            if (request instanceof ServerProtocol.Request.Open open) {
                var filePath = PathOpenService.normalizePath(open.absolutePath());
                var session = new ServerSession(clientChannel, filePath);

                // sessions の操作もロジックスレッドに統一する (スレッドセーフティ)
                actionQueue.put(() -> {
                    sessions.put(filePath, session);
                    pathOpenService.open(open.absolutePath(), bufferManager, frame);
                    var buffer = frame.getActiveWindow().getBuffer();
                    buffer.enableMinorMode(serverMinorModeFactory.get());
                    session.sendOpened(buffer.getName());
                });
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.WARNING, "クライアントリクエストの処理に失敗", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private @Nullable String readLine(SocketChannel channel) throws IOException {
        var buffer = ByteBuffer.allocate(4096);
        var bytesRead = channel.read(buffer);
        if (bytesRead <= 0) {
            return null;
        }
        buffer.flip();
        var content = StandardCharsets.UTF_8.decode(buffer).toString();
        // 改行を除去
        return content.stripTrailing();
    }

    private void sendResponse(SocketChannel channel, ServerProtocol.Response response) {
        var line = ServerProtocol.encode(response) + "\n";
        var buffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
        try {
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "レスポンスの送信に失敗", e);
        }
    }

    @Override
    public Optional<ServerSession> findByPath(Path path) {
        return Optional.ofNullable(sessions.get(path));
    }

    @Override
    public void removeByPath(Path path) {
        sessions.remove(path);
    }

    @Override
    public void close() {
        // 全アクティブセッションにエラー通知
        for (var session : sessions.valuesView()) {
            session.notifyError("server shutting down");
        }
        sessions.clear();

        // accept スレッドを停止
        if (acceptThread != null) {
            acceptThread.interrupt();
        }

        // サーバーソケットをクローズ
        if (serverChannel != null) {
            try {
                serverChannel.close();
            } catch (IOException e) {
                logger.log(Level.FINE, "サーバーソケットのクローズに失敗", e);
            }
        }

        // ソケットファイルを削除
        if (socketPath != null) {
            try {
                Files.deleteIfExists(socketPath);
            } catch (IOException e) {
                logger.log(Level.WARNING, "ソケットファイルの削除に失敗: " + socketPath, e);
            }
        }

        logger.info("サーバーを停止しました");
    }

    /**
     * stale なソケットファイルを検出して削除する。
     * 既存ソケットに接続テストを行い、接続できなければ stale と判断する。
     */
    static void cleanupStaleSocket(Path socketPath) throws IOException {
        if (!Files.exists(socketPath)) {
            return;
        }
        try {
            var address = UnixDomainSocketAddress.of(socketPath);
            var testChannel = SocketChannel.open(address);
            testChannel.close();
            // 接続できた → 既にサーバーが起動中
            throw new IOException("別の alle サーバーが既に起動中です: " + socketPath);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("別の alle サーバー")) {
                throw e;
            }
            // 接続できなかった → stale socket
            logger.info("stale ソケットファイルを削除します: " + socketPath);
            Files.deleteIfExists(socketPath);
        }
    }
}
