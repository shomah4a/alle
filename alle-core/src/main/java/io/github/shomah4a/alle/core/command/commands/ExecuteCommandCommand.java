package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.input.CommandNameCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * コマンドレジストリから名前でコマンドを検索し実行する。
 * Emacsのexecute-extended-command (M-x) に相当。
 * ミニバッファでコマンド名を入力し、Tab補完付きで実行する。
 * カレントバッファのモードスコープを考慮した名前解決を行う。
 */
public class ExecuteCommandCommand implements Command {

    private final CommandResolver commandResolver;
    private final InputHistory commandHistory;

    public ExecuteCommandCommand(CommandResolver commandResolver, InputHistory commandHistory) {
        this.commandResolver = commandResolver;
        this.commandHistory = commandHistory;
    }

    @Override
    public String name() {
        return "execute-command";
    }

    /**
     * 指定された名前のコマンドをカレントバッファのモードスコープで検索し実行する。
     * 解決されたコマンドの {@code execute} をそのまま呼び出す。
     * {@link io.github.shomah4a.alle.core.command.TransactionalCommand} であれば
     * コマンド自身がトランザクションを管理する。
     *
     * @throws IllegalArgumentException 指定された名前のコマンドが存在しない場合
     */
    public CompletableFuture<Void> executeByName(String commandName, CommandContext context) {
        var buffer = context.activeWindow().getBuffer();
        var command = commandResolver.resolve(commandName, buffer);
        if (command.isEmpty()) {
            throw new IllegalArgumentException("コマンド '" + commandName + "' は登録されていません");
        }
        return command.get().execute(context);
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var buffer = context.activeWindow().getBuffer();
        boolean ignoreCase = context.settingsRegistry().getEffective(EditorSettings.COMPLETION_IGNORE_CASE);
        var completer = new CommandNameCompleter(commandResolver, buffer, ignoreCase);
        return context.inputPrompter()
                .prompt("M-x ", "", commandHistory, completer)
                .thenCompose(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed
                            && !confirmed.value().isEmpty()) {
                        return executeByName(confirmed.value(), context);
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }
}
