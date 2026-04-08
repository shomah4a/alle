package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.VisualLineUtil;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * カーソルを次の行に移動するコマンド。
 * Emacsのnext-lineに相当する。
 * 移動先の行が現在のカラム位置より短い場合は行末に移動する。
 * 折り返しモード時は視覚行単位で移動する。
 */
public class NextLineCommand implements Command {

    @Override
    public String name() {
        return "next-line";
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
        int lineCount = buffer.lineCount();

        if (window.isTruncateLines()) {
            if (currentLine >= lineCount - 1) {
                return CompletableFuture.completedFuture(null);
            }
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int column = point - currentLineStart;
            int nextLineStart = buffer.lineStartOffset(currentLine + 1);
            int nextLineLength =
                    (int) buffer.lineText(currentLine + 1).codePoints().count();
            int newColumn = Math.min(column, nextLineLength);
            window.setPoint(nextLineStart + newColumn);
        } else {
            int columns = window.getViewportSize().columns();
            int tabWidth = buffer.getSettings().get(EditorSettings.TAB_WIDTH);
            int currentLineStart = buffer.lineStartOffset(currentLine);
            int cpOffset = point - currentLineStart;
            String lineText = buffer.lineText(currentLine);
            int currentVisualLine = VisualLineUtil.computeVisualLineForOffset(lineText, columns, cpOffset, tabWidth);
            int visualLineCount = VisualLineUtil.computeVisualLineCount(lineText, columns, tabWidth);

            // 視覚行内でのカラム位置を計算
            int vlStartCp = VisualLineUtil.visualLineStartOffset(lineText, columns, currentVisualLine, tabWidth);
            int cursorCol = DisplayWidthUtil.computeColumnForOffset(lineText, cpOffset, tabWidth)
                    - DisplayWidthUtil.computeColumnForOffset(lineText, vlStartCp, tabWidth);

            if (currentVisualLine < visualLineCount - 1) {
                // 同一バッファ行内の次の視覚行へ
                int nextVlStartCp =
                        VisualLineUtil.visualLineStartOffset(lineText, columns, currentVisualLine + 1, tabWidth);
                int nextVlEndCp =
                        VisualLineUtil.visualLineEndOffset(lineText, columns, currentVisualLine + 1, tabWidth);
                int newCp = computeCpForColumn(lineText, nextVlStartCp, nextVlEndCp, cursorCol, tabWidth);
                window.setPoint(currentLineStart + newCp);
            } else if (currentLine < lineCount - 1) {
                // 次のバッファ行の最初の視覚行へ
                int nextLineStart = buffer.lineStartOffset(currentLine + 1);
                String nextLineText = buffer.lineText(currentLine + 1);
                int nextVlEndCp = VisualLineUtil.visualLineEndOffset(nextLineText, columns, 0, tabWidth);
                int newCp = computeCpForColumn(nextLineText, 0, nextVlEndCp, cursorCol, tabWidth);
                window.setPoint(nextLineStart + newCp);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 指定カラム位置に最も近いコードポイントオフセットを返す。
     * 範囲内で指定カラムに収まる最大のオフセットを返す。
     */
    static int computeCpForColumn(String lineText, int startCp, int endCp, int targetColumn, int tabWidth) {
        int startCol = DisplayWidthUtil.computeColumnForOffset(lineText, startCp, tabWidth);
        int offset = 0;
        int cpIndex = 0;

        // startCpまでスキップ
        while (offset < lineText.length() && cpIndex < startCp) {
            int codePoint = lineText.codePointAt(offset);
            offset += Character.charCount(codePoint);
            cpIndex++;
        }

        int resultCp = startCp;
        while (offset < lineText.length() && cpIndex < endCp) {
            int codePoint = lineText.codePointAt(offset);
            int currentCol = DisplayWidthUtil.computeColumnForOffset(lineText, cpIndex, tabWidth) - startCol;
            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint, currentCol, tabWidth);
            if (currentCol + displayWidth > targetColumn) {
                break;
            }
            offset += Character.charCount(codePoint);
            cpIndex++;
            resultCp = cpIndex;
        }
        return Math.min(resultCp, endCp);
    }
}
