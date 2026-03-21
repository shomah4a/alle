package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.input.CommandNameCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.util.concurrent.CompletableFuture;

/**
 * コマンドレジストリから名前でコマンドを検索し実行する。
 * Emacsのexecute-extended-command (M-x) に相当。
 * ミニバッファでコマンド名を入力し、Tab補完付きで実行する。
 */
public class ExecuteCommandCommand implements Command {

    private final CommandRegistry registry;
    private final InputHistory commandHistory;

    public ExecuteCommandCommand(CommandRegistry registry, InputHistory commandHistory) {
        this.registry = registry;
        this.commandHistory = commandHistory;
    }

    @Override
    public String name() {
        return "execute-command";
    }

    /**
     * 指定された名前のコマンドを検索し実行する。
     *
     * @throws IllegalArgumentException 指定された名前のコマンドが存在しない場合
     */
    public CompletableFuture<Void> executeByName(String commandName, CommandContext context) {
        var command = registry.lookup(commandName);
        if (command.isEmpty()) {
            throw new IllegalArgumentException("コマンド '" + commandName + "' は登録されていません");
        }
        return command.get().execute(context);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var completer = new CommandNameCompleter(registry);
        return context.inputPrompter()
                .prompt("M-x ", "", completer, commandHistory)
                .thenCompose(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed
                            && !confirmed.value().isEmpty()) {
                        return executeByName(confirmed.value(), context);
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }
}
