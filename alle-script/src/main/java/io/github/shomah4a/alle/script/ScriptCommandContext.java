package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.list.ListIterable;

/**
 * スクリプトに公開するコマンド実行コンテキスト。
 * Java側のCommandContextをラップし、スクリプト向けのファサードを提供する。
 */
public class ScriptCommandContext {

    private final CommandContext ctx;

    private ScriptCommandContext(CommandContext ctx) {
        this.ctx = ctx;
    }

    /**
     * CommandContextからScriptCommandContextを生成する。
     * GraalPyからはコンストラクタを直接呼べないためstaticファクトリを提供する。
     */
    public static ScriptCommandContext of(CommandContext ctx) {
        return new ScriptCommandContext(ctx);
    }

    /**
     * アクティブウィンドウのファサードを返す。
     */
    public WindowFacade activeWindow() {
        return new WindowFacade(ctx.activeWindow());
    }

    /**
     * アクティブウィンドウのバッファのファサードを返す。
     */
    public BufferFacade currentBuffer() {
        return new BufferFacade(ctx.activeWindow().getBuffer());
    }

    /**
     * エコーエリアにメッセージを表示する。
     */
    public void message(String text) {
        ctx.messageBuffer().message(text);
    }

    /**
     * コマンドを発動したキーシーケンスを返す。
     * プログラム的呼び出し時は空リストを返す。
     */
    public ListIterable<KeyStroke> triggeringKeySequence() {
        return ctx.triggeringKeySequence();
    }

    /**
     * 名前を指定して別のコマンドを実行する。
     * コンテキストはそのまま渡されるため、thisCommandやlastCommandは変わらない。
     *
     * @throws IllegalArgumentException 指定された名前のコマンドが登録されていない場合
     */
    public CompletableFuture<Void> delegate(String commandName) {
        return ctx.delegate(commandName);
    }
}
