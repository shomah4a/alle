package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.Loggable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;
import org.jspecify.annotations.Nullable;

/**
 * {@link InteractiveShellProcess} の ProcessBuilder ベースのデフォルト実装。
 *
 * <p>{@code /bin/bash --noediting -i} で対話的シェルを起動する。
 * stdout/stderr は {@code redirectErrorStream(true)} でマージし、
 * デーモンスレッドで行単位に読み取る。
 *
 * <p>環境変数 {@code TERM=xterm-256color} を設定し、色情報を受け取れるようにする。
 */
public class DefaultInteractiveShellProcess implements InteractiveShellProcess, Loggable {

    private @Nullable Process process;
    private @Nullable Writer stdinWriter;
    private @Nullable Thread readerThread;

    @Override
    public void start(Path workingDirectory, Consumer<String> onOutputLine) {
        try {
            var builder = new ProcessBuilder("/bin/bash", "--noediting", "-i");
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.environment().put("TERM", "xterm-256color");

            var proc = builder.start();
            this.process = proc;
            this.stdinWriter = new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8);

            var reader = new Thread(() -> readOutput(proc, onOutputLine), "shell-reader-" + proc.pid());
            reader.setDaemon(true);
            reader.start();
            this.readerThread = reader;
        } catch (IOException e) {
            throw new IllegalStateException("シェルプロセスの起動に失敗", e);
        }
    }

    private void readOutput(Process proc, Consumer<String> onOutputLine) {
        try (var reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                onOutputLine.accept(line);
            }
        } catch (IOException e) {
            if (proc.isAlive()) {
                logger().warn("シェル出力の読み取り中にエラーが発生", e);
            }
        }
    }

    @Override
    public void sendInput(String input) {
        var writer = this.stdinWriter;
        if (writer == null) {
            return;
        }
        try {
            writer.write(input + "\n");
            writer.flush();
        } catch (IOException e) {
            logger().warn("stdin への書き込みに失敗", e);
        }
    }

    @Override
    public void sendSignal(int signal) {
        var proc = this.process;
        if (proc == null) {
            return;
        }
        try {
            new ProcessBuilder("kill", "-" + signal, String.valueOf(proc.pid()))
                    .start()
                    .waitFor();
        } catch (IOException e) {
            logger().warn("シグナル送信に失敗: signal=" + signal, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isAlive() {
        var proc = this.process;
        return proc != null && proc.isAlive();
    }

    @Override
    public void destroy() {
        var proc = this.process;
        if (proc != null) {
            proc.destroyForcibly();
        }
        var thread = this.readerThread;
        if (thread != null) {
            thread.interrupt();
        }
    }

    @Override
    public long pid() {
        var proc = this.process;
        if (proc == null) {
            throw new IllegalStateException("プロセスが開始されていません");
        }
        return proc.pid();
    }
}
