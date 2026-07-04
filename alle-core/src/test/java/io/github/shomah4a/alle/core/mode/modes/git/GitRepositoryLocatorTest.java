package io.github.shomah4a.alle.core.mode.modes.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitRepositoryLocatorTest {

    @Test
    void 引数ディレクトリ直下にgitディレクトリがある場合そのディレクトリが返る(@TempDir Path tempDir) throws IOException {
        Files.createDirectory(tempDir.resolve(".git"));

        var locator = new GitRepositoryLocator();

        assertEquals(Optional.of(tempDir), locator.locate(tempDir));
    }

    @Test
    void 引数ディレクトリ直下にgitがファイルの場合そのディレクトリが返る(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve(".git"), "gitdir: /somewhere/else\n");

        var locator = new GitRepositoryLocator();

        assertEquals(Optional.of(tempDir), locator.locate(tempDir));
    }

    @Test
    void 引数ファイルの親ディレクトリにgitがある場合親ディレクトリが返る(@TempDir Path tempDir) throws IOException {
        Files.createDirectory(tempDir.resolve(".git"));
        var srcFile = tempDir.resolve("src.txt");
        Files.writeString(srcFile, "");

        var locator = new GitRepositoryLocator();

        assertEquals(Optional.of(tempDir), locator.locate(srcFile));
    }

    @Test
    void 数段深いネストからでも祖先のリポジトリルートが返る(@TempDir Path tempDir) throws IOException {
        Files.createDirectory(tempDir.resolve(".git"));
        var deep = tempDir.resolve("a").resolve("b").resolve("c");
        Files.createDirectories(deep);
        var srcFile = deep.resolve("s.txt");
        Files.writeString(srcFile, "");

        var locator = new GitRepositoryLocator();

        assertEquals(Optional.of(tempDir), locator.locate(srcFile));
    }

    @Test
    void gitが見つからない場合emptyが返る(@TempDir Path tempDir) throws IOException {
        var deep = tempDir.resolve("a").resolve("b");
        Files.createDirectories(deep);

        var locator = new GitRepositoryLocator();

        assertTrue(locator.locate(deep).isEmpty());
    }

    @Test
    void 同一Pathの二回目の呼び出しはリゾルバが再実行されない(@TempDir Path tempDir) {
        var callCount = new AtomicInteger(0);
        var locator = new GitRepositoryLocator(Duration.ofSeconds(60), 100, path -> {
            callCount.incrementAndGet();
            return Optional.of(tempDir);
        });

        locator.locate(tempDir);
        locator.locate(tempDir);

        assertEquals(1, callCount.get());
    }

    @Test
    void 異なるPathのそれぞれについてリゾルバが1回ずつ呼ばれる(@TempDir Path tempDir) {
        var callCount = new AtomicInteger(0);
        var locator = new GitRepositoryLocator(Duration.ofSeconds(60), 100, path -> {
            callCount.incrementAndGet();
            return Optional.of(tempDir);
        });

        locator.locate(tempDir.resolve("a"));
        locator.locate(tempDir.resolve("b"));

        assertEquals(2, callCount.get());
    }
}
