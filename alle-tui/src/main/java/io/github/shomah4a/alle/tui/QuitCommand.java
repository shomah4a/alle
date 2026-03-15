package io.github.shomah4a.alle.tui;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * エディタを終了するコマンド。
 * TerminalInputSourceのshutdownフラグを立てて、CommandLoopを自然に終了させる。
 */
public class QuitCommand implements Command {

    private final TerminalInputSource inputSource;

    public QuitCommand(TerminalInputSource inputSource) {
        this.inputSource = inputSource;
    }

    @Override
    public String name() {
        return "quit";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        inputSource.requestShutdown();
        return CompletableFuture.completedFuture(null);
    }
}
