package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.input.ShellCommandExecutor;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * テスト用のShellCommandExecutorスタブ。
 * 呼び出されたコマンドと作業ディレクトリを記録し、設定されたstdout/stderr/exitCodeを返す。
 */
class StubShellCommandExecutor implements ShellCommandExecutor {

    final MutableList<String> executedCommands = Lists.mutable.empty();
    final MutableList<Path> workingDirectories = Lists.mutable.empty();
    private MutableList<String> stdoutLines = Lists.mutable.empty();
    private MutableList<String> stderrLines = Lists.mutable.empty();
    private int exitCode;

    void setStdout(String... lines) {
        this.stdoutLines = Lists.mutable.of(lines);
    }

    void setStderr(String... lines) {
        this.stderrLines = Lists.mutable.of(lines);
    }

    void setExitCode(int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public CompletableFuture<Integer> execute(
            String command, Path workingDirectory, Consumer<String> onStdoutLine, Consumer<String> onStderrLine) {
        executedCommands.add(command);
        workingDirectories.add(workingDirectory);
        for (String line : stdoutLines) {
            onStdoutLine.accept(line);
        }
        for (String line : stderrLines) {
            onStderrLine.accept(line);
        }
        return CompletableFuture.completedFuture(exitCode);
    }
}
