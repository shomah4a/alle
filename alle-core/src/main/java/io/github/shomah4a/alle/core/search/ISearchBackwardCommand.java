package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * 後方インクリメンタルサーチを開始するコマンド（C-r）。
 */
public class ISearchBackwardCommand implements Command {

    private final ISearchHistory history;

    public ISearchBackwardCommand(ISearchHistory history) {
        this.history = history;
    }

    @Override
    public String name() {
        return "isearch-backward";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var session = new ISearchSession(
                context.activeWindow(), context.messageBuffer(), context.overridingKeymapController(), history, false);
        session.start();
        return CompletableFuture.completedFuture(null);
    }
}
