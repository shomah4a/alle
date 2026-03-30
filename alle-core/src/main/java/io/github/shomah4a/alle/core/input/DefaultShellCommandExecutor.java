package io.github.shomah4a.alle.core.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * /bin/sh -c 経由でシェルコマンドを非同期に実行するデフォルト実装。
 * stdout と stderr は別スレッドで並行読み取りし、行単位でハンドラに通知する。
 */
public class DefaultShellCommandExecutor implements ShellCommandExecutor {

    @Override
    public CompletableFuture<Integer> execute(
            String command, Path workingDirectory, Consumer<String> onStdoutLine, Consumer<String> onStderrLine) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return run(command, workingDirectory, onStdoutLine, onStderrLine);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static int run(
            String command, Path workingDirectory, Consumer<String> onStdoutLine, Consumer<String> onStderrLine)
            throws IOException {
        var processBuilder = new ProcessBuilder("/bin/sh", "-c", command);
        processBuilder.directory(workingDirectory.toFile());

        var process = processBuilder.start();
        try {
            CompletableFuture<Void> stderrFuture = CompletableFuture.runAsync(() -> {
                try (var reader =
                        new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        onStderrLine.accept(line);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            try (var reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    onStdoutLine.accept(line);
                }
            }

            stderrFuture.join();
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("コマンドの実行が中断されました", e);
        }
    }
}
