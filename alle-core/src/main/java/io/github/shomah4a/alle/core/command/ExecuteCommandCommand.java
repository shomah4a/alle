package io.github.shomah4a.alle.core.command;

/**
 * コマンドレジストリから名前でコマンドを検索し実行する。
 * Emacsのexecute-extended-command (M-x) に相当する基盤。
 * 現時点ではプログラム的な呼び出しのみサポートし、
 * ミニバッファUIによるインタラクティブな名前入力は別途実装する。
 */
public class ExecuteCommandCommand implements Command {

    private final CommandRegistry registry;

    public ExecuteCommandCommand(CommandRegistry registry) {
        this.registry = registry;
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
    public void executeByName(String commandName, CommandContext context) {
        var command = registry.lookup(commandName);
        if (command.isEmpty()) {
            throw new IllegalArgumentException("コマンド '" + commandName + "' は登録されていません");
        }
        command.get().execute(context);
    }

    @Override
    public void execute(CommandContext context) {
        // ミニバッファUIが実装されるまでは何もしない
    }
}
