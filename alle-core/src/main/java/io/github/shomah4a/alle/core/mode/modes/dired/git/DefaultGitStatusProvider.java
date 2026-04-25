package io.github.shomah4a.alle.core.mode.modes.dired.git;

import io.github.shomah4a.alle.core.Loggable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;

/**
 * git��マンドを実行してリポジトリ情報を取得する実装。
 */
public class DefaultGitStatusProvider implements GitStatusProvider, Loggable {

    @Override
    public MapIterable<Path, String> getFileStatuses(Path rootDirectory) {
        MutableMap<Path, String> statuses = Maps.mutable.empty();
        try {
            var process = new ProcessBuilder("git", "status", "--porcelain=v1")
                    .directory(rootDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            try (var reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() < 4) {
                        continue;
                    }
                    String statusCode = parseStatusCode(line.charAt(0), line.charAt(1));
                    String relativePath = line.substring(3);
                    // ��ネーム表記 "R  old -> new" の場合は新しいパスを使う
                    int arrowIndex = relativePath.indexOf(" -> ");
                    if (arrowIndex >= 0) {
                        relativePath = relativePath.substring(arrowIndex + 4);
                    }
                    Path absolutePath = rootDirectory.resolve(relativePath).normalize();
                    statuses.put(absolutePath, statusCode);
                }
            }
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger().debug("git status の実行に失敗: " + rootDirectory, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return statuses;
    }

    @Override
    public String getBranch(Path rootDirectory) {
        try {
            var process = new ProcessBuilder("git", "branch", "--show-current")
                    .directory(rootDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            try (var reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                process.waitFor();
                if (line != null && !line.isEmpty()) {
                    return line.trim();
                }
            }
            // デタッチドHEADの場合は短縮ハッシュを取得
            var hashProcess = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .directory(rootDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            try (var reader =
                    new BufferedReader(new InputStreamReader(hashProcess.getInputStream(), StandardCharsets.UTF_8))) {
                String hashLine = reader.readLine();
                hashProcess.waitFor();
                if (hashLine != null && !hashLine.isEmpty()) {
                    return hashLine.trim();
                }
            }
        } catch (IOException | InterruptedException e) {
            logger().debug("git branch の取得に失敗: " + rootDirectory, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        return "";
    }

    @Override
    public void stageFiles(Path rootDirectory, ListIterable<Path> files) {
        if (files.isEmpty()) {
            return;
        }
        var command = Lists.mutable.of("git", "add", "--");
        for (Path file : files) {
            command.add(file.toString());
        }
        try {
            var process = new ProcessBuilder(command)
                    .directory(rootDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger().debug("git add の実行に失敗: " + rootDirectory, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isTracked(Path rootDirectory, Path file) {
        try {
            var process = new ProcessBuilder("git", "ls-files", "--error-unmatch", "--", file.toString())
                    .directory(rootDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            logger().debug("git ls-files の実行に失敗: " + file, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }

    @Override
    public void removeFiles(Path rootDirectory, ListIterable<Path> files, boolean force) {
        if (files.isEmpty()) {
            return;
        }
        var command = Lists.mutable.of("git", "rm");
        if (force) {
            command.add("-rf");
        }
        command.add("--");
        for (Path file : files) {
            command.add(file.toString());
        }
        try {
            var process = new ProcessBuilder(command)
                    .directory(rootDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger().debug("git rm の実行に失敗: " + rootDirectory, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void moveFile(Path rootDirectory, Path source, Path destination) {
        try {
            var process = new ProcessBuilder("git", "mv", "--", source.toString(), destination.toString())
                    .directory(rootDirectory.toFile())
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger().debug("git mv の実行に失敗: " + source, e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String parseStatusCode(char indexStatus, char workTreeStatus) {
        // イ���デックス側のステータスを優先的に表示
        if (indexStatus == 'A') {
            return "A";
        }
        if (indexStatus == 'D' || workTreeStatus == 'D') {
            return "D";
        }
        if (indexStatus == 'R') {
            return "R";
        }
        if (indexStatus == 'M' || workTreeStatus == 'M') {
            return "M";
        }
        if (indexStatus == '?' && workTreeStatus == '?') {
            return "?";
        }
        if (indexStatus == '!' && workTreeStatus == '!') {
            return "!";
        }
        return String.valueOf(workTreeStatus).trim();
    }
}
