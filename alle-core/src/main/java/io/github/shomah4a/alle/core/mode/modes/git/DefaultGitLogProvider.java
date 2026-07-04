package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.Loggable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
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
        this(DefaultGitLogProvider::runProcess);
    }

    DefaultGitLogProvider(ProcessRunner processRunner) {
        this.processRunner = processRunner;
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
                entries.add(new GitLogEntry(
                        shortHash, OffsetDateTime.parse(isoTime).toInstant(), author, subject, body));
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
            Loggable.createLogger(DefaultGitLogProvider.class).debug("git コマンドの実行に失敗: {}", e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Loggable.createLogger(DefaultGitLogProvider.class).debug("git コマンドの実行が割り込まれました: {}", e.getMessage());
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
