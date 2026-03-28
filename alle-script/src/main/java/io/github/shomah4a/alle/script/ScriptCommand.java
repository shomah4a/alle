package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;
import org.graalvm.polyglot.Value;

/**
 * スクリプト側のコマンド関数をラップするCommand実装。
 *
 * <p>Java側でCommandContextをScriptCommandContextに変換してから
 * スクリプト側の関数を呼び出す。Python側でのJavaクラスインスタンス化を不要にする。
 */
public class ScriptCommand implements Command {

    private final String commandName;
    private final Value runFunction;

    public ScriptCommand(String commandName, Value runFunction) {
        this.commandName = commandName;
        this.runFunction = runFunction;
    }

    @Override
    public String name() {
        return commandName;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        try {
            ScriptCommandContext scriptCtx = ScriptCommandContext.of(context);
            runFunction.execute(scriptCtx);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
