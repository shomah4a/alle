package io.github.shomah4a.alle.core.server;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServerManagerTest {

    @TempDir
    Path tempDir;

    private ServerManager manager;
    private Path socketPath;

    @BeforeEach
    void setUp() {
        manager = new ServerManager();
        socketPath = tempDir.resolve("test-server.sock");
    }

    @AfterEach
    void tearDown() {
        manager.close();
    }

    @Test
    void staleソケットファイルが存在する場合は削除される() throws IOException {
        // stale なソケットファイルを作成 (通常のファイルとして)
        Files.createFile(socketPath);
        assertTrue(Files.exists(socketPath));

        ServerManager.cleanupStaleSocket(socketPath);
        assertFalse(Files.exists(socketPath));
    }

    @Test
    void closeでソケットファイルが削除される() throws Exception {
        var testSocketPath = tempDir.resolve("close-test.sock");
        setSocketPath(testSocketPath);

        // ソケットファイルを模擬的に作成
        Files.createFile(testSocketPath);
        assertTrue(Files.exists(testSocketPath));

        manager.close();
        assertFalse(Files.exists(testSocketPath));
    }

    /**
     * テスト用にSocketPathフィールドを設定する。
     */
    private void setSocketPath(Path path) throws Exception {
        var field = ServerManager.class.getDeclaredField("socketPath");
        field.setAccessible(true);
        field.set(manager, path);
    }
}
