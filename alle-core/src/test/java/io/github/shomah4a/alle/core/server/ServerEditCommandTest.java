package io.github.shomah4a.alle.core.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerEditCommandTest {

    @TempDir
    Path tempDir;

    private SettingsRegistry settingsRegistry;
    private BufferFacade scratch;
    private BufferFacade targetBuffer;
    private Frame frame;
    private BufferManager bufferManager;
    private boolean saveInvoked;
    private BufferIO bufferIO;
    private StubSessionLookup sessionLookup;
    private ServerEditCommand cmd;

    @BeforeEach
    void setUp() {
        settingsRegistry = new SettingsRegistry();
        scratch = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), settingsRegistry));

        var targetPath = tempDir.resolve("COMMIT_EDITMSG");
        targetBuffer =
                new BufferFacade(new TextBuffer("COMMIT_EDITMSG", new GapTextModel(), settingsRegistry, targetPath));

        var window = new Window(targetBuffer);
        var minibuffer =
                new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settingsRegistry)));
        frame = new Frame(window, minibuffer);

        bufferManager = new BufferManager();
        bufferManager.add(scratch);
        bufferManager.add(targetBuffer);

        saveInvoked = false;
        bufferIO = new BufferIO(
                source -> new StringReader(""),
                destination -> {
                    saveInvoked = true;
                    return new StringWriter();
                },
                settingsRegistry);

        sessionLookup = new StubSessionLookup();
        cmd = new ServerEditCommand(bufferIO, sessionLookup);
    }

    @Test
    void serverモードなしのバッファではメッセージを表示して何もしない() {
        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        // バッファは残っている
        assertTrue(bufferManager.findByName("COMMIT_EDITMSG").isPresent());
    }

    @Test
    void serverモード付きバッファを保存してセッションに完了通知しバッファをkillする() throws IOException {
        // server マイナーモードを有効化
        var serverEditCmd = new ServerEditCommand(bufferIO, sessionLookup);
        var serverMode = new ServerMinorMode(serverEditCmd);
        targetBuffer.enableMinorMode(serverMode);

        // セッションを登録
        var pair = createSocketPair();
        var session =
                new ServerSession(pair.server(), targetBuffer.getFilePath().orElseThrow());
        sessionLookup.register(targetBuffer.getFilePath().orElseThrow(), session);

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        // バッファが保存された
        assertTrue(saveInvoked);

        // セッションに完了通知が送られた
        assertTrue(session.completion().isDone());

        // クライアント側でFINISHEDを受信
        var received = readFromChannel(pair.client());
        assertTrue(received.contains("FINISHED"));

        // バッファが kill された
        assertFalse(bufferManager.findByName("COMMIT_EDITMSG").isPresent());

        // セッションが除去された
        assertTrue(sessionLookup
                .findByPath(targetBuffer.getFilePath().orElseThrow())
                .isEmpty());

        pair.close();
    }

    @Test
    void バッファが1つしかない場合はkillせず保存と通知のみ行う() throws IOException {
        // scratch を除去して targetBuffer のみにする
        bufferManager.remove("*scratch*");
        assertEquals(1, bufferManager.size());

        var serverMode = new ServerMinorMode(cmd);
        targetBuffer.enableMinorMode(serverMode);

        var pair = createSocketPair();
        var session =
                new ServerSession(pair.server(), targetBuffer.getFilePath().orElseThrow());
        sessionLookup.register(targetBuffer.getFilePath().orElseThrow(), session);

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        // セッションに完了通知が送られた
        assertTrue(session.completion().isDone());

        // バッファは残っている (kill されない)
        assertTrue(bufferManager.findByName("COMMIT_EDITMSG").isPresent());

        pair.close();
    }

    private SocketPair createSocketPair() throws IOException {
        var socketPath = tempDir.resolve("test-" + System.nanoTime() + ".sock");
        var address = UnixDomainSocketAddress.of(socketPath);
        var serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        serverChannel.bind(address);
        var clientChannel = SocketChannel.open(address);
        var acceptedChannel = serverChannel.accept();
        serverChannel.close();
        Files.deleteIfExists(socketPath);
        return new SocketPair(acceptedChannel, clientChannel);
    }

    private String readFromChannel(SocketChannel channel) throws IOException {
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

    static class StubSessionLookup implements ServerSessionLookup {
        private final MutableMap<Path, ServerSession> sessions = Maps.mutable.empty();

        void register(Path path, ServerSession session) {
            sessions.put(path, session);
        }

        @Override
        public Optional<ServerSession> findByPath(Path path) {
            return Optional.ofNullable(sessions.get(path));
        }

        @Override
        public void removeByPath(Path path) {
            sessions.remove(path);
        }
    }
}
