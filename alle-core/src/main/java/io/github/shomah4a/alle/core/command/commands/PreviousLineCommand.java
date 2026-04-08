package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.VisualLineUtil;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * カーソルを前の行に移動するコマンド。
 * Emacsのprevious-lineに相当する。
 * 移動先の行が現在のカラム位置より短い場合は行末に移動する。
 * 折り返しモード時は視覚行単位で移動する。
 */
public class PreviousLineCommand implements Command {

    @Override
    public String name() {
        return "previous-line";
    }

    @Override
    public boolean keepsRegionActive() {
        return true;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int currentLine = buffer.lineIndexForOffset(point);

        if (window.isTruncateLines()) {
            if (currentLine <= 0) {
                return CompletableFuture.completedFuture(null);
            }
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int column = point - currentLineStart;
            int prevLineStart = buffer.lineStartOffset(currentLine - 1);
            int prevLineLength =
                    (int) buffer.lineText(currentLine - 1).codePoints().count();
            int newColumn = Math.min(column, prevLineLength);
            window.setPoint(prevLineStart + newColumn);
        } else {
            int columns = window.getViewportSize().columns();
            int tabWidth = buffer.getSettings().get(EditorSettings.TAB_WIDTH);
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int cpOffset = point - currentLineStart;
            String lineText = buffer.lineText(currentLine);
            int currentVisualLine = VisualLineUtil.computeVisualLineForOffset(lineText, columns, cpOffset, tabWidth);

            // 視覚行内でのカラム位置を計算
            int vlStartCp = VisualLineUtil.visualLineStartOffset(lineText, columns, currentVisualLine, tabWidth);
            int cursorCol = DisplayWidthUtil.computeColumnForOffset(lineText, cpOffset, tabWidth)
                    - DisplayWidthUtil.computeColumnForOffset(lineText, vlStartCp, tabWidth);

            if (currentVisualLine > 0) {
                // 同一バッファ行内の前の視覚行へ
                int prevVlStartCp =
                        VisualLineUtil.visualLineStartOffset(lineText, columns, currentVisualLine - 1, tabWidth);
                int prevVlEndCp =
                        VisualLineUtil.visualLineEndOffset(lineText, columns, currentVisualLine - 1, tabWidth);
                int newCp =
                        NextLineCommand.computeCpForColumn(lineText, prevVlStartCp, prevVlEndCp, cursorCol, tabWidth);
                window.setPoint(currentLineStart + newCp);
            } else if (currentLine > 0) {
                // 前のバッファ行の最後の視覚行へ
                int prevLine = currentLine - 1;
                int prevLineStart = buffer.lineStartOffset(prevLine);
                String prevLineText = buffer.lineText(prevLine);
                int prevVisualLineCount = VisualLineUtil.computeVisualLineCount(prevLineText, columns, tabWidth);
                int lastVl = prevVisualLineCount - 1;
                int lastVlStartCp = VisualLineUtil.visualLineStartOffset(prevLineText, columns, lastVl, tabWidth);
                int lastVlEndCp = VisualLineUtil.visualLineEndOffset(prevLineText, columns, lastVl, tabWidth);
                int newCp = NextLineCommand.computeCpForColumn(
                        prevLineText, lastVlStartCp, lastVlEndCp, cursorCol, tabWidth);
                window.setPoint(prevLineStart + newCp);
            }
        }
        return CompletableFuture.completedFuture(null);
    }
}
