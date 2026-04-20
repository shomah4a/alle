package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * query-replace 対話中にセッションをキャンセルするコマンド（C-g）。
 * ここまでの置換結果は保持される。
 */
public class QueryReplaceCancelCommand implements Command {

    private final QueryReplaceSession session;

    public QueryReplaceCancelCommand(QueryReplaceSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "query-replace-cancel";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        session.cancel();
        return CompletableFuture.completedFuture(null);
    }
}
