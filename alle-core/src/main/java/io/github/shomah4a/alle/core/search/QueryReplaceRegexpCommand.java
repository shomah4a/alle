package io.github.shomah4a.alle.core.search;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正規表現に対する対話型置換コマンド (Emacs C-M-%)。
 * ミニバッファで FROM (パターン) / TO (展開テンプレート) を順に入力し、
 * {@link QueryReplaceSession} を regex モードで起動する。
 */
public class QueryReplaceRegexpCommand implements Command {

    private final InputHistory fromHistory;
    private final InputHistory toHistory;

    public QueryReplaceRegexpCommand(InputHistory fromHistory, InputHistory toHistory) {
        this.fromHistory = fromHistory;
        this.toHistory = toHistory;
    }

    @Override
    public String name() {
        return "query-replace-regexp";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.inputPrompter()
                .prompt("Query replace regexp: ", fromHistory)
                .thenCompose(fromResult -> {
                    if (!(fromResult instanceof PromptResult.Confirmed fromConfirmed)
                            || fromConfirmed.value().isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    String patternSource = fromConfirmed.value();
                    Pattern compiled;
                    try {
                        compiled = Pattern.compile(patternSource);
                    } catch (PatternSyntaxException ex) {
                        context.handleError("Invalid regexp: " + ex.getDescription(), ex);
                        return CompletableFuture.completedFuture(null);
                    }
                    return context.inputPrompter()
                            .prompt("Query replace regexp " + patternSource + " with: ", toHistory)
                            .thenAccept(toResult -> {
                                if (!(toResult instanceof PromptResult.Confirmed toConfirmed)) {
                                    return;
                                }
                                startSession(context, compiled, toConfirmed.value());
                            });
                });
    }

    private void startSession(CommandContext context, Pattern pattern, String toValue) {
        var window = context.activeWindow();
        var regionStart = window.getRegionStart();
        var regionEnd = window.getRegionEnd();
        boolean regionActive = regionStart.isPresent() && regionEnd.isPresent();

        int start = regionActive ? regionStart.get() : window.getPoint();
        int end = regionActive ? regionEnd.get() : window.getBuffer().length();

        var session = QueryReplaceSession.forRegexp(
                window,
                context.messageBuffer(),
                context.overridingKeymapController(),
                pattern,
                toValue,
                start,
                end,
                regionActive);
        session.start();
    }
}
