package io.github.shomah4a.alle.core.command;

/**
 * カーソルから行末まで削除するコマンド。
 * 行末にいる場合は改行文字を削除して次の行と結合する。
 * バッファ末尾では何もしない。
 * Emacsのkill-lineに相当する（kill-ringへの蓄積は未実装）。
 */
public class KillLineCommand implements Command {

    @Override
    public String name() {
        return "kill-line";
    }

    @Override
    public void execute(CommandContext context) {
        var window = context.frame().getActiveWindow();
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int bufferLength = buffer.length();

        if (point >= bufferLength) {
            return;
        }

        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        String lineText = buffer.lineText(lineIndex);
        int lineLength = (int) lineText.codePoints().count();
        int lineEnd = lineStart + lineLength;

        if (point < lineEnd) {
            window.deleteForward(lineEnd - point);
        } else {
            window.deleteForward(1);
        }
    }
}
