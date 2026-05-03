package io.github.shomah4a.alle.core.input;

/**
 * 対話的な長期実行シェルプロセスのインターフェース。
 * テスト時にスタブ実装に差し替え可能とするため、プロセス管理を抽象化する。
 *
 * <p>既存の {@link ShellCommandExecutor}（一回実行型）とは異なり、
 * プロセスを起動したまま stdin への書き込みやシグナル送信を繰り返し行う。
 *
 * <p>インスタンス生成時にプロセスは起動済みの状態となる。
 */
public interface InteractiveShellProcess {

    /**
     * プロセスの stdin にテキストを送信する。末尾に改行が付加される。
     *
     * @param input 送信するテキスト（改行を含まない）
     */
    void sendInput(String input);

    /**
     * プロセスにシグナルを送信する。
     *
     * @param signal シグナル番号（例: 2 = SIGINT, 20 = SIGTSTP）
     */
    void sendSignal(int signal);

    /**
     * プロセスが実行中であるかを返す。
     */
    boolean isAlive();

    /**
     * プロセスを終了する。リーダースレッドも停止する。
     */
    void destroy();

    /**
     * プロセスの PID を返す。
     */
    long pid();
}
