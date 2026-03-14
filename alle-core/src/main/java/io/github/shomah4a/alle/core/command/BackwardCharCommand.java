package io.github.shomah4a.alle.core.command;

/**
 * カーソルを1文字後方に移動するコマンド。
 * Emacsのbackward-charに相当する。
 */
public class BackwardCharCommand implements Command {

    @Override
    public String name() {
        return "backward-char";
    }

    @Override
    public void execute(CommandContext context) {
        var window = context.frame().getActiveWindow();
        int point = window.getPoint();
        if (point > 0) {
            window.setPoint(point - 1);
        }
    }
}
