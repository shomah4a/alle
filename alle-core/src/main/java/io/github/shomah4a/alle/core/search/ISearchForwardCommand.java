package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * 前方インクリメンタルサーチを開始するコマンド（C-s）。
 */
public class ISearchForwardCommand implements Command {

    private final ISearchHistory history;

    public ISearchForwardCommand(ISearchHistory history) {
        this.history = history;
    }

    @Override
    public String name() {
        return "isearch-forward";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var session = new ISearchSession(
                context.activeWindow(), context.messageBuffer(), context.overridingKeymapController(), history, true);
        session.start();
        return CompletableFuture.completedFuture(null);
    }
}
