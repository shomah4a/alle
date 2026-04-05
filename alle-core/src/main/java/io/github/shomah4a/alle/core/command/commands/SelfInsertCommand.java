package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * トリガーキーのキーコードに対応する文字をカーソル位置に挿入するコマンド。
 * Emacsのself-insert-commandに相当する。
 * 修飾キー付きやコードポイントが無効な場合は何もしない。
 */
public class SelfInsertCommand implements Command {

    @Override
    public String name() {
        return "self-insert-command";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var keySequence = context.triggeringKeySequence();
        if (keySequence.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        var key = keySequence.getLast();
        if (!key.modifiers().isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        int codePoint = key.keyCode();
        if (!Character.isValidCodePoint(codePoint) || Character.getType(codePoint) == Character.CONTROL) {
            return CompletableFuture.completedFuture(null);
        }
        context.activeWindow().insert(Character.toString(codePoint));
        return CompletableFuture.completedFuture(null);
    }
}
