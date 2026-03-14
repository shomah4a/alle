package io.github.shomah4a.alle.core.command;

/**
 * カーソルを1文字前方に移動するコマンド。
 * Emacsのforward-charに相当する。
 */
public class ForwardCharCommand implements Command {

    @Override
    public String name() {
        return "forward-char";
    }

    @Override
    public void execute(CommandContext context) {
        var window = context.frame().getActiveWindow();
        int point = window.getPoint();
        int length = window.getBuffer().length();
        if (point < length) {
            window.setPoint(point + 1);
        }
    }
}
