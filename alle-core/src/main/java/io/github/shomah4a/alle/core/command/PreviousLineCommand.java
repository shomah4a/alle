package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを前の行に移動するコマンド。
 * Emacsのprevious-lineに相当する。
 * 移動先の行が現在のカラム位置より短い場合は行末に移動する。
 */
public class PreviousLineCommand implements Command {

    @Override
    public String name() {
        return "previous-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().moveToPreviousLine();
    }
}
