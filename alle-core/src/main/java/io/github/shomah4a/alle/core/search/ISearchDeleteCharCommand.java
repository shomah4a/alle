package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * i-searchのクエリから末尾の文字を削除するコマンド（DEL/Backspace）。
 */
class ISearchDeleteCharCommand implements Command {

    private final ISearchSession session;

    ISearchDeleteCharCommand(ISearchSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "isearch-delete-char";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        session.deleteChar();
        return CompletableFuture.completedFuture(null);
    }
}
