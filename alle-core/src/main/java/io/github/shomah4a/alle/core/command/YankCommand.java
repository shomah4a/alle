package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * kill-ringの最新エントリをカーソル位置に挿入するコマンド。
 * Emacsのyank (C-y) に相当する。
 * kill-ringが空の場合は何もしない。
 */
public class YankCommand implements Command {

    @Override
    public String name() {
        return "yank";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().atomicPerform(window -> {
            var textOpt = context.killRing().current();
            if (textOpt.isEmpty()) {
                return null;
            }
            window.insert(textOpt.get());
            return null;
        });
    }
}
