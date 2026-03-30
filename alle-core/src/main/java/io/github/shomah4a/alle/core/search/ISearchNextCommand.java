package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * i-search中に次のマッチに移動するコマンド（C-s）。
 */
class ISearchNextCommand implements Command {

    private final ISearchSession session;

    ISearchNextCommand(ISearchSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "isearch-repeat-forward";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        session.searchNext();
        return CompletableFuture.completedFuture(null);
    }
}
