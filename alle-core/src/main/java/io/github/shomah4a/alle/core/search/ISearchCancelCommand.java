package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * i-searchをキャンセルするコマンド（C-g）。
 * カーソルを元の位置に戻し、ハイライトを除去する。
 */
class ISearchCancelCommand implements Command {

    private final ISearchSession session;

    ISearchCancelCommand(ISearchSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "isearch-abort";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        session.cancel();
        return CompletableFuture.completedFuture(null);
    }
}
