package io.github.shomah4a.alle.core.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.statusline.GitBranchProvider.GitBranchInfo;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CachingGitBranchProviderTest {

    @Test
    void 同一リポジトリルートへの連続呼び出しではdelegateが1回だけ呼ばれる() {
        var callCount = new AtomicInteger(0);
        GitBranchProvider delegate = path -> {
            callCount.incrementAndGet();
            return Optional.of(new GitBranchInfo("main", false));
        };
        // gitRootResolverは固定のルートを返す
        Path fixedRoot = Path.of("/repo");
        var caching = new CachingGitBranchProvider(delegate, Duration.ofSeconds(60), 100, path -> fixedRoot);

        var result1 = caching.getBranch(Path.of("/repo/src/A.java"));
        var result2 = caching.getBranch(Path.of("/repo/src/B.java"));

        assertTrue(result1.isPresent());
        assertTrue(result2.isPresent());
        assertEquals("main", result1.get().branchName());
        assertEquals(1, callCount.get());
    }

    @Test
    void 異なるリポジトリルートでは別々にdelegateが呼ばれる() {
        var callCount = new AtomicInteger(0);
        GitBranchProvider delegate = path -> {
            callCount.incrementAndGet();
            return Optional.of(new GitBranchInfo("main", false));
        };
        // ファイルパスの親ディレクトリをルートとして返す
        var caching = new CachingGitBranchProvider(delegate, Duration.ofSeconds(60), 100, path -> path.getParent());

        caching.getBranch(Path.of("/repo1/test.txt"));
        caching.getBranch(Path.of("/repo2/test.txt"));

        assertEquals(2, callCount.get());
    }

    @Test
    void gitRootResolverがnullを返した場合はemptyを返す() {
        GitBranchProvider delegate = path -> Optional.of(new GitBranchInfo("main", false));
        var caching = new CachingGitBranchProvider(delegate, Duration.ofSeconds(60), 100, path -> null);

        var result = caching.getBranch(Path.of("/tmp/test.txt"));
        assertTrue(result.isEmpty());
    }
}
