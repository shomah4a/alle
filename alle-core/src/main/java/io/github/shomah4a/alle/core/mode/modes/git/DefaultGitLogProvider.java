package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.Loggable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * git log をサブプロセスで実行して {@link GitLogEntry} 列を返すデフォルト実装。
 * 出力は US(0x1f) 区切り / RS(0x1e) 終端でパースする。
 */
public class DefaultGitLogProvider implements GitLogProvider, Loggable {

    private static final int TIMEOUT_SECONDS = 3;
    private static final String US = String.valueOf((char) 0x1F);
    private static final String RS = String.valueOf((char) 0x1E);
    private static final String PRETTY_FORMAT = "--pretty=format:%h%x1f%aI%x1f%an%x1f%s%x1f%b%x1e";

    private final ProcessRunner processRunner;

    public DefaultGitLogProvider() {
        this(defaultProcessRunner(DefaultGitLogProvider::logStderrLine));
    }

    /**
     * stderr の 1 行ごとに指定コンシューマを呼ぶよう構築する。
     * 通常は EditorCore から *Warnings* バッファへ流す用途で使う。
     */
    public DefaultGitLogProvider(Consumer<String> stderrLineConsumer) {
        this(defaultProcessRunner(stderrLineConsumer));
    }

    DefaultGitLogProvider(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    private static ProcessRunner defaultProcessRunner(Consumer<String> stderrLineConsumer) {
        return (dir, cmd) -> runProcess(dir, stderrLineConsumer, cmd);
    }

    private static void logStderrLine(String line) {
        Loggable.createLogger(DefaultGitLogProvider.class).warn("git stderr: {}", line);
    }

    @Override
    public ImmutableList<GitLogEntry> getLog(Path repoRoot, Optional<Path> target, int skip, int maxCount) {
        var command = Lists.mutable.of("git", "log", "--max-count=" + maxCount);
        if (skip > 0) {
            command.add("--skip=" + skip);
        }
        command.add(PRETTY_FORMAT);
        if (target.isPresent()) {
            command.add("--");
            command.add(target.get().toString());
        }
        var result = processRunner.run(repoRoot, command.toArray(new String[0]));
        return result.map(this::parse).orElse(Lists.immutable.empty());
    }

    private ImmutableList<GitLogEntry> parse(String output) {
        var entries = Lists.mutable.<GitLogEntry>empty();
        for (var rawRecord : output.split(RS, -1)) {
            // rawRecord の先頭に前 record からの改行が付くケースがあるため、
            // shortHash 側でのみ ASCII 空白を落とす。US/RS は Java の isWhitespace 対象なので
            // rawRecord 全体には strip をかけない (末尾フィールドが失われる)。
            if (rawRecord.isEmpty()) {
                continue;
            }
            var fields = rawRecord.split(US, -1);
            if (fields.length < 5) {
                logger().warn("git log 出力の破損レコードをスキップ: fields={}", fields.length);
                continue;
            }
            String shortHash = trimAsciiWhitespace(fields[0]);
            String isoTime = fields[1];
            String author = fields[2];
            String subject = fields[3];
            String body = fields[4];
            if (shortHash.isEmpty()) {
                continue;
            }
            try {
                entries.add(
                        new GitLogEntry(shortHash, OffsetDateTime.parse(isoTime).toInstant(), author, subject, body));
            } catch (DateTimeParseException e) {
                logger().warn("git log 出力の commit time パースに失敗: {}", isoTime, e);
            }
        }
        return entries.toImmutable();
    }

    private static String trimAsciiWhitespace(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isAsciiSpace(value.charAt(start))) {
            start++;
        }
        while (end > start && isAsciiSpace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isAsciiSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }

    private static Optional<String> runProcess(
            Path workingDir, Consumer<String> stderrLineConsumer, String... command) {
        Process process = null;
        java.util.concurrent.CompletableFuture<String> stdoutReader = null;
        java.util.concurrent.CompletableFuture<Void> stderrReader = null;
        try {
            var pb = new ProcessBuilder(command);
            pb.directory(workingDir.toFile());
            // stderr は merge せず、専用スレッドで drain して stderrLineConsumer に流す。
            process = pb.start();

            // 出力読み取りを別スレッドで走らせる。readAllBytes を先に呼ぶと
            // プロセスが stdout を閉じるまでこのスレッドが無条件ブロックし、
            // 後段の waitFor(timeout) が実効を失うため。
            final var stdoutStream = process.getInputStream();
            stdoutReader = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return new String(stdoutStream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    return "";
                }
            });

            // stderr も並行して drain しないと OS パイプのバッファ満杯で
            // プロセスが書き込みブロックしうる。
            final var stderrStream = process.getErrorStream();
            stderrReader = java.util.concurrent.CompletableFuture.runAsync(() -> {
                try (var reader = new BufferedReader(new InputStreamReader(stderrStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderrLineConsumer.accept(line);
                    }
                } catch (IOException e) {
                    // stderr 読み取り中断は握りつぶす (プロセスが destroyForcibly される経路)
                }
            });

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                stdoutReader.cancel(true);
                stderrReader.cancel(true);
                return Optional.empty();
            }
            if (process.exitValue() != 0) {
                stdoutReader.cancel(true);
                stderrReader.cancel(true);
                return Optional.empty();
            }
            // プロセスは終了したので両ストリームも EOF に達しているはず。
            // 予期せぬハングに備えて短い timeout を設ける。
            String output = stdoutReader.get(1, TimeUnit.SECONDS);
            try {
                stderrReader.get(1, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException ignored) {
                stderrReader.cancel(true);
            }
            return Optional.of(output);
        } catch (IOException e) {
            Loggable.createLogger(DefaultGitLogProvider.class).debug("git コマンドの実行に失敗: {}", e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Loggable.createLogger(DefaultGitLogProvider.class).debug("git コマンドの実行が割り込まれました: {}", e.getMessage());
            if (process != null) {
                process.destroyForcibly();
            }
            if (stdoutReader != null) {
                stdoutReader.cancel(true);
            }
            if (stderrReader != null) {
                stderrReader.cancel(true);
            }
            return Optional.empty();
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException e) {
            Loggable.createLogger(DefaultGitLogProvider.class).debug("git コマンドの出力読み取りに失敗: {}", e.getMessage());
            if (process != null) {
                process.destroyForcibly();
            }
            if (stdoutReader != null) {
                stdoutReader.cancel(true);
            }
            if (stderrReader != null) {
                stderrReader.cancel(true);
            }
            return Optional.empty();
        }
    }

    /**
     * 外部プロセス実行の抽象化。テスト時にスタブを注入するために使用する。
     */
    @FunctionalInterface
    interface ProcessRunner {
        Optional<String> run(Path workingDir, String... command);
    }
}
