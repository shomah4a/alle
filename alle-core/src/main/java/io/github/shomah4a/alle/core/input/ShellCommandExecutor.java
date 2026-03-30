package io.github.shomah4a.alle.core.input;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * シェルコマンドを実行するインターフェース。
 * テスト時にスタブ実装に差し替え可能とするため、プロセス実行を抽象化する。
 */
public interface ShellCommandExecutor {

    /**
     * 指定されたコマンド文字列をシェル経由で非同期に実行する。
     * stdout/stderr は行単位でハンドラに通知される。
     *
     * @param command 実行するコマンド文字列
     * @param workingDirectory 作業ディレクトリ
     * @param onStdoutLine stdout の各行を受け取るハンドラ
     * @param onStderrLine stderr の各行を受け取るハンドラ
     * @return exit code の Future
     */
    CompletableFuture<Integer> execute(
            String command, Path workingDirectory, Consumer<String> onStdoutLine, Consumer<String> onStderrLine);
}
