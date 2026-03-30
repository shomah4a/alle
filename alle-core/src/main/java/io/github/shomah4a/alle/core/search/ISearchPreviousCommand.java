package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * i-search中に前のマッチに移動するコマンド（C-r）。
 */
class ISearchPreviousCommand implements Command {

    private final ISearchSession session;

    ISearchPreviousCommand(ISearchSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "isearch-repeat-backward";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        session.searchPrevious();
        return CompletableFuture.completedFuture(null);
    }
}
