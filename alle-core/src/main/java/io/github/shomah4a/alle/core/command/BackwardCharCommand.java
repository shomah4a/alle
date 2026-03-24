package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを1文字後方に移動するコマンド。
 * Emacsのbackward-charに相当する。
 */
public class BackwardCharCommand implements Command {

    @Override
    public String name() {
        return "backward-char";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().moveBackward();
    }
}
