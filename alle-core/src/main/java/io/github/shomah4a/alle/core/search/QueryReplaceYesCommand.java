package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * query-replace 対話中に現在のマッチを置換するコマンド（y / SPC）。
 */
public class QueryReplaceYesCommand implements Command {

    private final QueryReplaceSession session;

    public QueryReplaceYesCommand(QueryReplaceSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "query-replace-yes";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        session.replaceCurrent();
        return CompletableFuture.completedFuture(null);
    }
}
