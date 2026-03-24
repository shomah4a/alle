package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルを次の行に移動するコマンド。
 * Emacsのnext-lineに相当する。
 * 移動先の行が現在のカラム位置より短い場合は行末に移動する。
 */
public class NextLineCommand implements Command {

    @Override
    public String name() {
        return "next-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().moveToNextLine();
    }
}
