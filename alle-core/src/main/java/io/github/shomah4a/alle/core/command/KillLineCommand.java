package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルから行末まで削除するコマンド。
 * 行末にいる場合は改行文字を削除して次の行と結合する。
 * バッファ末尾では何もしない。
 * 削除したテキストはkill-ringに蓄積される。
 * 連続実行時は前回のエントリに追記する。
 * Emacsのkill-lineに相当する。
 */
public class KillLineCommand implements Command {

    @Override
    public String name() {
        return "kill-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().atomicPerform(window -> {
            var buffer = window.getBuffer();
            int point = window.getPoint();
            int bufferLength = buffer.length();

            if (point >= bufferLength) {
                return null;
            }

            int lineIndex = buffer.lineIndexForOffset(point);
            int lineStart = buffer.lineStartOffset(lineIndex);
            String lineText = buffer.lineText(lineIndex);
            int lineLength = (int) lineText.codePoints().count();
            int lineEnd = lineStart + lineLength;

            int deleteCount;
            if (point < lineEnd) {
                deleteCount = lineEnd - point;
            } else {
                deleteCount = 1;
            }

            String killedText = buffer.substring(point, point + deleteCount);
            window.deleteForward(deleteCount);

            boolean isConsecutiveKill =
                    context.lastCommand().map(last -> last.equals(name())).orElse(false);
            if (isConsecutiveKill) {
                context.killRing().appendToLast(killedText);
            } else {
                context.killRing().push(killedText);
            }
            return null;
        });
    }
}
