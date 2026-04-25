package io.github.shomah4a.alle.core.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerSessionTest {

    @TempDir
    Path tempDir;

    @Test
    void notifyFinishedでFINISHEDが送信されFutureが完了する() throws IOException {
        try (var pair = createSocketPair()) {
            var session = new ServerSession(pair.server(), Path.of("/tmp/test.txt"));

            assertFalse(session.completion().isDone());

            session.notifyFinished();

            assertTrue(session.completion().isDone());

            var received = readLine(pair.client());
            assertTrue(received.contains("FINISHED"));
        }
    }

    @Test
    void sendOpenedでOPENEDが送信される() throws IOException {
        try (var pair = createSocketPair()) {
            var session = new ServerSession(pair.server(), Path.of("/tmp/test.txt"));

            session.sendOpened("test.txt");

            var received = readLine(pair.client());
            assertTrue(received.contains("OPENED test.txt"));
        }
    }

    @Test
    void notifyErrorでERRORが送信されFutureが例外で完了する() throws IOException {
        try (var pair = createSocketPair()) {
            var session = new ServerSession(pair.server(), Path.of("/tmp/test.txt"));

            session.notifyError("something failed");

            assertTrue(session.completion().isCompletedExceptionally());

            var received = readLine(pair.client());
            assertTrue(received.contains("ERROR something failed"));
        }
    }

    @Test
    void filePathが取得できる() throws IOException {
        try (var pair = createSocketPair()) {
            var path = Path.of("/tmp/COMMIT_EDITMSG");
            var session = new ServerSession(pair.server(), path);
            assertTrue(session.filePath().equals(path));
        }
    }

    private SocketPair createSocketPair() throws IOException {
        var socketPath = tempDir.resolve("test.sock");
        var address = UnixDomainSocketAddress.of(socketPath);
        var serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(address);

        var clientChannel = SocketChannel.open(address);
        var acceptedChannel = serverChannel.accept();

        serverChannel.close();
        Files.deleteIfExists(socketPath);

        return new SocketPair(acceptedChannel, clientChannel);
    }

    private String readLine(SocketChannel channel) throws IOException {
        var buffer = ByteBuffer.allocate(1024);
        channel.read(buffer);
        buffer.flip();
        return StandardCharsets.UTF_8.decode(buffer).toString();
    }

    record SocketPair(SocketChannel server, SocketChannel client) implements AutoCloseable {
        @Override
        public void close() throws IOException {
            server.close();
            client.close();
        }
    }
}
