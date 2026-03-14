package io.github.shomah4a.alle.core.command;

/**
 * カーソルを行末に移動するコマンド。
 * Emacsのend-of-lineに相当する。
 */
public class EndOfLineCommand implements Command {

    @Override
    public String name() {
        return "end-of-line";
    }

    @Override
    public void execute(CommandContext context) {
        var window = context.frame().getActiveWindow();
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        String lineText = buffer.lineText(lineIndex);
        int lineLength = (int) lineText.codePoints().count();
        window.setPoint(lineStart + lineLength);
    }
}
