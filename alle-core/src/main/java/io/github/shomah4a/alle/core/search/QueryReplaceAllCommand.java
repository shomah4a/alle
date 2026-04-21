package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.TransactionalCommand;
import java.util.concurrent.CompletableFuture;

/**
 * query-replace 対話中に以降の全マッチを無確認で置換するコマンド（!）。
 */
public class QueryReplaceAllCommand implements TransactionalCommand {

    private final QueryReplaceSession session;

    public QueryReplaceAllCommand(QueryReplaceSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "query-replace-all";
    }

    @Override
    public CompletableFuture<Void> executeInTransaction(CommandContext context) {
        session.replaceAllRemaining();
        return CompletableFuture.completedFuture(null);
    }
}
