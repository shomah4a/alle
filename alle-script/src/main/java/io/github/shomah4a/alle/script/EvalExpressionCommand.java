package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.util.concurrent.CompletableFuture;

/**
 * スクリプトを評価するコマンド。EmacsのM-: (eval-expression)に相当。
 * ミニバッファでコードを入力し、ScriptEngineで評価する。
 * 結果はエコーエリアに表示し、エラーは*Warnings*にも記録する。
 */
public class EvalExpressionCommand implements Command {

    private final ScriptEngine scriptEngine;

    public EvalExpressionCommand(ScriptEngine scriptEngine) {
        this.scriptEngine = scriptEngine;
    }

    @Override
    public String name() {
        return "eval-expression";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.inputPrompter().prompt("Eval: ").thenCompose(result -> {
            if (result instanceof PromptResult.Confirmed confirmed
                    && !confirmed.value().isEmpty()) {
                return evaluateAndDisplay(confirmed.value(), context);
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private CompletableFuture<Void> evaluateAndDisplay(String code, CommandContext context) {
        ScriptResult scriptResult = scriptEngine.eval(code);
        switch (scriptResult) {
            case ScriptResult.Success success -> {
                if (!success.value().isEmpty()) {
                    context.messageBuffer().message(success.value());
                }
            }
            case ScriptResult.Failure failure -> {
                context.handleError("Script error: " + failure.message(), failure.cause());
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
