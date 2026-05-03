package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.input.InteractiveShellProcess;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * {@link InteractiveShellProcess} の生成を抽象化するファクトリインターフェース。
 * テスト時にスタブプロセスに差し替え可能とする。
 */
@FunctionalInterface
interface ShellProcessFactory {

    /**
     * 対話的シェルプロセスを起動して返す。
     *
     * @param workingDirectory 作業ディレクトリ
     * @param onOutputLine 出力行のコールバック
     * @param onProcessExit プロセス終了時のコールバック
     * @return 起動済みのプロセスインスタンス
     */
    InteractiveShellProcess create(Path workingDirectory, Consumer<String> onOutputLine, Runnable onProcessExit);
}
