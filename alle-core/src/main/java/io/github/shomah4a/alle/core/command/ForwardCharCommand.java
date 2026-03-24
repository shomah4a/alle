package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを1文字前方に移動するコマンド。
 * Emacsのforward-charに相当する。
 */
public class ForwardCharCommand implements Command {

    @Override
    public String name() {
        return "forward-char";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().moveForward();
    }
}
