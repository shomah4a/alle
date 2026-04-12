package io.github.shomah4a.alle.core.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DefaultGitBranchProviderTest {

    @Test
    void ブランチ名とクリーン状態を取得できる() {
        DefaultGitBranchProvider.ProcessRunner runner = (workingDir, command) -> {
            String cmd = String.join(" ", command);
            if (cmd.contains("rev-parse")) {
                return Optional.of("main\n");
            }
            if (cmd.contains("status")) {
                return Optional.of("");
            }
            return Optional.empty();
        };
        var provider = new DefaultGitBranchProvider(runner);
        var result = provider.getBranch(Path.of("/repo/src/test.txt"));

        assertTrue(result.isPresent());
        assertEquals("main", result.get().branchName());
        assertFalse(result.get().dirty());
    }

    @Test
    void ワーキングツリーに変更がある場合はdirtyがtrueになる() {
        DefaultGitBranchProvider.ProcessRunner runner = (workingDir, command) -> {
            String cmd = String.join(" ", command);
            if (cmd.contains("rev-parse")) {
                return Optional.of("develop\n");
            }
            if (cmd.contains("status")) {
                return Optional.of(" M src/main.java\n");
            }
            return Optional.empty();
        };
        var provider = new DefaultGitBranchProvider(runner);
        var result = provider.getBranch(Path.of("/repo/src/test.txt"));

        assertTrue(result.isPresent());
        assertTrue(result.get().dirty());
    }

    @Test
    void rev_parseが失敗した場合はemptyを返す() {
        DefaultGitBranchProvider.ProcessRunner runner = (workingDir, command) -> Optional.empty();
        var provider = new DefaultGitBranchProvider(runner);
        var result = provider.getBranch(Path.of("/tmp/test.txt"));

        assertTrue(result.isEmpty());
    }

    @Test
    void ルートパスの場合はemptyを返す() {
        DefaultGitBranchProvider.ProcessRunner runner = (workingDir, command) -> Optional.of("main\n");
        var provider = new DefaultGitBranchProvider(runner);
        var result = provider.getBranch(Path.of("/"));

        // "/" のparentはnull
        assertTrue(result.isEmpty());
    }
}
