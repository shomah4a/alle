package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * query-replace 対話中に現在のマッチをスキップするコマンド（n / DEL）。
 */
public class QueryReplaceNoCommand implements Command {

    private final QueryReplaceSession session;

    public QueryReplaceNoCommand(QueryReplaceSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "query-replace-no";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        session.skipCurrent();
        return CompletableFuture.completedFuture(null);
    }
}
