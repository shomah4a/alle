package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.util.concurrent.CompletableFuture;

/**
 * リテラル文字列に対する対話型置換コマンド (Emacs M-%)。
 * ミニバッファで FROM / TO を順に入力し、{@link QueryReplaceSession} を起動する。
 */
public class QueryReplaceCommand implements Command {

    private final InputHistory fromHistory;
    private final InputHistory toHistory;

    public QueryReplaceCommand(InputHistory fromHistory, InputHistory toHistory) {
        this.fromHistory = fromHistory;
        this.toHistory = toHistory;
    }

    @Override
    public String name() {
        return "query-replace";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.inputPrompter().prompt("Query replace: ", fromHistory).thenCompose(fromResult -> {
            if (!(fromResult instanceof PromptResult.Confirmed fromConfirmed)
                    || fromConfirmed.value().isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            String fromValue = fromConfirmed.value();
            return context.inputPrompter()
                    .prompt("Query replace " + fromValue + " with: ", toHistory)
                    .thenAccept(toResult -> {
                        if (!(toResult instanceof PromptResult.Confirmed toConfirmed)) {
                            return;
                        }
                        startSession(context, fromValue, toConfirmed.value());
                    });
        });
    }

    private void startSession(CommandContext context, String fromValue, String toValue) {
        var window = context.activeWindow();
        var regionStart = window.getRegionStart();
        var regionEnd = window.getRegionEnd();
        boolean regionActive = regionStart.isPresent() && regionEnd.isPresent();

        int start = regionActive ? regionStart.get() : window.getPoint();
        int end = regionActive ? regionEnd.get() : window.getBuffer().length();

        var session = QueryReplaceSession.forLiteral(
                window,
                context.messageBuffer(),
                context.overridingKeymapController(),
                fromValue,
                toValue,
                start,
                end,
                regionActive);
        session.start();
    }
}
