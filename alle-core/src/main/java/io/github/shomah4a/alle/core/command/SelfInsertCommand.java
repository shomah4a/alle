package io.github.shomah4a.alle.core.command;

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
    public void execute(CommandContext context) {
        var keyOpt = context.triggeringKey();
        if (keyOpt.isEmpty()) {
            return;
        }
        var key = keyOpt.get();
        if (!key.modifiers().isEmpty()) {
            return;
        }
        int codePoint = key.keyCode();
        if (!Character.isValidCodePoint(codePoint) || Character.getType(codePoint) == Character.CONTROL) {
            return;
        }
        var window = context.frame().getActiveWindow();
        window.insert(Character.toString(codePoint));
    }
}
