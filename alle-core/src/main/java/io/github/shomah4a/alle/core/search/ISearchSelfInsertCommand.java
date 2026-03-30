package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * i-search中の文字入力コマンド。
 * 入力された文字をクエリに追加し、インクリメンタル検索を実行する。
 */
class ISearchSelfInsertCommand implements Command {

    private final ISearchSession session;

    ISearchSelfInsertCommand(ISearchSession session) {
        this.session = session;
    }

    @Override
    public String name() {
        return "isearch-self-insert";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        context.triggeringKey().ifPresent(key -> session.appendChar(key.keyCode()));
        return CompletableFuture.completedFuture(null);
    }
}
