package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * 行頭の空白領域でBackspace時にインデント単位で削除するコマンド。
 * カーソルが行頭の空白領域にある場合、インデント幅分まとめて削除する。
 * それ以外の位置では通常の1文字削除。
 */
public class IndentDedentBackspaceCommand implements Command {

    @Override
    public String name() {
        return "indent-dedent-backspace";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        int point = window.getPoint();
        if (point == 0) {
            return CompletableFuture.completedFuture(null);
        }

        int indentWidth = buffer.getSettings().get(EditorSettings.INDENT_WIDTH);
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int col = point - lineStart;
        String lineText = buffer.lineText(lineIndex);

        // 行頭の空白の長さを計算
        int indentLen = 0;
        for (int i = 0; i < lineText.length(); i++) {
            if (lineText.charAt(i) == ' ' || lineText.charAt(i) == '\t') {
                indentLen++;
            } else {
                break;
            }
        }

        if (col > 0 && col <= indentLen) {
            int deleteCount = col % indentWidth;
            if (deleteCount == 0) {
                deleteCount = indentWidth;
            }
            deleteCount = Math.min(deleteCount, col);
            window.deleteBackward(deleteCount);
        } else {
            window.deleteBackward(1);
        }
        return CompletableFuture.completedFuture(null);
    }
}
