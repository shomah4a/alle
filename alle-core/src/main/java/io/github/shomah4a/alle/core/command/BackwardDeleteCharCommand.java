package io.github.shomah4a.alle.core.command;

/**
 * カーソル前の文字を削除するコマンド。
 * Emacsのbackward-delete-charに相当する。
 */
public class BackwardDeleteCharCommand implements Command {

    @Override
    public String name() {
        return "backward-delete-char";
    }

    @Override
    public void execute(CommandContext context) {
        context.frame().getActiveWindow().deleteBackward(1);
    }
}
