package io.github.shomah4a.alle.core.command;

/**
 * カーソル位置の文字を削除するコマンド。
 * Emacsのdelete-charに相当する。
 */
public class DeleteCharCommand implements Command {

    @Override
    public String name() {
        return "delete-char";
    }

    @Override
    public void execute(CommandContext context) {
        context.frame().getActiveWindow().deleteForward(1);
    }
}
