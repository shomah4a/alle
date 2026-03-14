package io.github.shomah4a.alle.core.command;

/**
 * カーソルを行頭に移動するコマンド。
 * Emacsのbeginning-of-lineに相当する。
 */
public class BeginningOfLineCommand implements Command {

    @Override
    public String name() {
        return "beginning-of-line";
    }

    @Override
    public void execute(CommandContext context) {
        var window = context.frame().getActiveWindow();
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        window.setPoint(lineStart);
    }
}
