package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * i-searchを確定するコマンド（RET）。
 * カーソルを現在位置に残し、ハイライトを除去する。
 */
class ISearchConfirmCommand implements Command {

    private final ISearchSession session;

    ISearchConfirmCommand(ISearchSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "isearch-exit";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        session.confirm();
        return CompletableFuture.completedFuture(null);
    }
}
