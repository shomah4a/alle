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
import org.slf4j.Logger;

/**
 * {@link InteractiveShellProcess} の ProcessBuilder ベースのデフォルト実装。
 *
 * <p>環境変数 {@code SHELL} で指定されたシェル（未設定の場合は {@code /bin/sh}）を
 * {@code script} コマンド経由で疑似PTY付きで起動する。
 * これにより {@code -i}（対話モード）を使用でき、ターミナルエミュレータと同様の動作となる。
 *
 * <p>stdout/stderr は {@code redirectErrorStream(true)} でマージし、
 * デーモンスレッドで行単位に読み取る。
 *
 * <p>環境変数 {@code TERM=xterm-256color} を設定し、色情報を受け取れるようにする。
 *
 * <p>インスタンス生成にはファクトリメソッド {@link #start(Path, Consumer, Runnable)} を使用する。
 * コンストラクタ時点でプロセスは起動済みとなるため、フィールドは non-null が保証される。
 */
public class DefaultInteractiveShellProcess implements InteractiveShellProcess {

    private static final Logger logger = Loggable.createLogger(DefaultInteractiveShellProcess.class);

    private final Process process;
    private final Writer stdinWriter;
    private final Thread readerThread;

    private DefaultInteractiveShellProcess(Process process, Writer stdinWriter, Thread readerThread) {
        this.process = process;
        this.stdinWriter = stdinWriter;
        this.readerThread = readerThread;
    }

    /**
     * 対話的シェルプロセスを起動して返す。
     *
     * <p>{@code script -q /dev/null -c "<shell> -i"} で疑似PTYを確保し、
     * シェルを対話モードで起動する。
     *
     * @param workingDirectory 作業ディレクトリ
     * @param onOutputLine stdout/stderr の各行を受け取るハンドラ（バックグラウンドスレッドから呼ばれる）
     * @param onProcessExit プロセス終了時に呼ばれるコールバック（リーダースレッドから呼ばれる）
     * @return 起動済みのプロセスインスタンス
     */
    public static DefaultInteractiveShellProcess start(
            Path workingDirectory, Consumer<String> onOutputLine, Runnable onProcessExit) {
        try {
            String shell = resolveShell();
            // stty -echo でPTYのエコーを無効化した上でシェルを対話モードで起動する。
            // エディタ側の self-insert でユーザー入力はバッファに反映されるため、
            // PTYのエコーは不要。
            var builder = new ProcessBuilder("script", "-q", "/dev/null", "-c", "stty -echo; " + shell + " -i");
            builder.directory(workingDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.environment().put("TERM", "xterm-256color");

            var proc = builder.start();
            var writer = new OutputStreamWriter(proc.getOutputStream(), StandardCharsets.UTF_8);
            var reader = new Thread(() -> readOutput(proc, onOutputLine, onProcessExit), "shell-reader-" + proc.pid());
            reader.setDaemon(true);
            reader.start();

            return new DefaultInteractiveShellProcess(proc, writer, reader);
        } catch (IOException e) {
            throw new IllegalStateException("シェルプロセスの起動に失敗", e);
        }
    }

    private static String resolveShell() {
        String shell = System.getenv("SHELL");
        if (shell != null && !shell.isEmpty()) {
            return shell;
        }
        return "/bin/sh";
    }

    private static void readOutput(Process proc, Consumer<String> onOutputLine, Runnable onProcessExit) {
        try (var reader = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                onOutputLine.accept(line);
            }
        } catch (IOException e) {
            if (proc.isAlive()) {
                logger.warn("シェル出力の読み取り中にエラーが発生", e);
            }
        }
        onProcessExit.run();
    }

    @Override
    public void sendInput(String input) {
        try {
            stdinWriter.write(input + "\n");
            stdinWriter.flush();
        } catch (IOException e) {
            logger.warn("stdin への書き込みに失敗", e);
        }
    }

    @Override
    public void sendSignal(int signal) {
        try {
            new ProcessBuilder("kill", "-" + signal, String.valueOf(process.pid()))
                    .start()
                    .waitFor();
        } catch (IOException e) {
            logger.warn("シグナル送信に失敗: signal={}", signal, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public void destroy() {
        process.destroyForcibly();
        readerThread.interrupt();
    }

    @Override
    public long pid() {
        return process.pid();
    }
}
