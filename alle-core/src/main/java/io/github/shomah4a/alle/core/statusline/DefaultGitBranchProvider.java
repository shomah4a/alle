package io.github.shomah4a.alle.core.statusline;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gitコマンドを実行してブランチ情報を取得するデフォルト実装。
 */
public class DefaultGitBranchProvider implements GitBranchProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultGitBranchProvider.class);
    private static final int TIMEOUT_SECONDS = 3;

    private final ProcessRunner processRunner;

    public DefaultGitBranchProvider() {
        this(DefaultGitBranchProvider::runProcess);
    }

    DefaultGitBranchProvider(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    @Override
    public Optional<GitBranchInfo> getBranch(Path filePath) {
        Path directory = filePath.getParent();
        if (directory == null) {
            return Optional.empty();
        }

        // ブランチ名を取得
        var branchResult = processRunner.run(directory, "git", "rev-parse", "--abbrev-ref", "HEAD");
        if (branchResult.isEmpty()) {
            return Optional.empty();
        }
        String branchName = branchResult.get().strip();
        if (branchName.isEmpty()) {
            return Optional.empty();
        }

        // ワーキングツリーの変更有無を確認
        var statusResult = processRunner.run(directory, "git", "status", "--porcelain");
        boolean dirty = statusResult.isPresent() && !statusResult.get().strip().isEmpty();

        return Optional.of(new GitBranchInfo(branchName, dirty));
    }

    private static Optional<String> runProcess(Path workingDir, String... command) {
        try {
            var pb = new ProcessBuilder(command);
            pb.directory(workingDir.toFile());
            pb.redirectErrorStream(true);
            var process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                return Optional.empty();
            }
            return Optional.of(output);
        } catch (IOException e) {
            LOG.debug("git コマンドの実行に失敗: {}", e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.debug("git コマンドの実行が割り込まれました: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 外部プロセス実行の抽象化。テスト時にモックを注入するために使用する。
     */
    @FunctionalInterface
    interface ProcessRunner {
        Optional<String> run(Path workingDir, String... command);
    }
}
