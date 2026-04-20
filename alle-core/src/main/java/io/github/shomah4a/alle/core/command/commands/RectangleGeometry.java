package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * 矩形編集のジオメトリ計算ヘルパー。
 *
 * <p>表示カラム単位（タブは TAB_WIDTH で展開、全角文字は 2）で矩形を定義するが、
 * 境界が全角文字やタブの中央に落ちる場合はその文字を矩形に含める（外側にスナップ）。
 * これにより元のタブ/全角文字は破壊されない。
 *
 * <p>各行で実際に切り出されるコードポイント範囲は行ごとに異なりうる
 * （跨ぎ文字がある行は指定より広くなる）。
 */
final class RectangleGeometry {

    private RectangleGeometry() {}

    /**
     * Window の (mark, point) から矩形を導出する。
     * mark が未設定の場合は Optional.empty() を返す。
     */
    static Optional<Rectangle> fromRegion(Window window, int tabWidth) {
        var markOpt = window.getMark();
        if (markOpt.isEmpty()) {
            return Optional.empty();
        }
        int mark = markOpt.get();
        int point = window.getPoint();
        var buffer = window.getBuffer();

        int markLine = buffer.lineIndexForOffset(mark);
        int pointLine = buffer.lineIndexForOffset(point);
        int markCol = columnOfOffset(buffer, mark, markLine, tabWidth);
        int pointCol = columnOfOffset(buffer, point, pointLine, tabWidth);

        int startLine = Math.min(markLine, pointLine);
        int endLine = Math.max(markLine, pointLine);
        int leftCol = Math.min(markCol, pointCol);
        int rightCol = Math.max(markCol, pointCol);
        return Optional.of(new Rectangle(startLine, endLine, leftCol, rightCol));
    }

    /**
     * 指定オフセットが属する行の行頭を 0 とした表示カラムを返す。
     */
    static int columnOfOffset(BufferFacade buffer, int offset, int lineIndex, int tabWidth) {
        int lineStart = buffer.lineStartOffset(lineIndex);
        String lineText = buffer.lineText(lineIndex);
        int cpOffset = offset - lineStart;
        return DisplayWidthUtil.computeColumnForOffset(lineText, cpOffset, tabWidth);
    }

    /**
     * 行テキスト内で targetColumn に対応するコードポイントオフセットを、
     * 「targetColumn 以下の最大の文字境界」として返す（round down）。
     *
     * <p>targetColumn がタブや全角文字の中央に落ちる場合、その文字の手前（左端）を返す。
     */
    static int columnRoundDown(String lineText, int targetColumn, int tabWidth) {
        int offset = 0;
        int cpIndex = 0;
        int col = 0;
        while (offset < lineText.length()) {
            int codePoint = lineText.codePointAt(offset);
            int width = DisplayWidthUtil.getDisplayWidth(codePoint, col, tabWidth);
            if (col + width > targetColumn) {
                break;
            }
            col += width;
            offset += Character.charCount(codePoint);
            cpIndex++;
            if (col >= targetColumn) {
                break;
            }
        }
        return cpIndex;
    }

    /**
     * 行テキスト内で targetColumn に対応するコードポイントオフセットを、
     * 「targetColumn 以上の最小の文字境界」として返す（round up）。
     *
     * <p>targetColumn がタブや全角文字の中央に落ちる場合、その文字の右端を返す。
     * 行末に達して targetColumn に届かない場合は行末を返す。
     */
    static int columnRoundUp(String lineText, int targetColumn, int tabWidth) {
        int offset = 0;
        int cpIndex = 0;
        int col = 0;
        while (offset < lineText.length() && col < targetColumn) {
            int codePoint = lineText.codePointAt(offset);
            int width = DisplayWidthUtil.getDisplayWidth(codePoint, col, tabWidth);
            col += width;
            offset += Character.charCount(codePoint);
            cpIndex++;
        }
        return cpIndex;
    }

    /**
     * 行 lineIndex の矩形境界に対応するコードポイントオフセット範囲を返す。
     * 左は round down、右は round up した結果。
     */
    static CpRange cpRangeForLine(BufferFacade buffer, int lineIndex, Rectangle rect, int tabWidth) {
        String lineText = buffer.lineText(lineIndex);
        int leftCp = columnRoundDown(lineText, rect.leftCol(), tabWidth);
        int rightCp = columnRoundUp(lineText, rect.rightCol(), tabWidth);
        if (rightCp < leftCp) {
            rightCp = leftCp;
        }
        return new CpRange(leftCp, rightCp);
    }

    /**
     * 矩形の内容を行毎の文字列として取り出す（非破壊）。
     */
    static ImmutableList<String> extractRectangle(BufferFacade buffer, Rectangle rect, int tabWidth) {
        MutableList<String> lines = Lists.mutable.empty();
        for (int li = rect.startLine(); li <= rect.endLine(); li++) {
            String lineText = buffer.lineText(li);
            var range = cpRangeForLine(buffer, li, rect, tabWidth);
            lines.add(codePointSubstring(lineText, range.leftCp(), range.rightCp()));
        }
        return lines.toImmutable();
    }

    /**
     * 矩形を削除する。各行の矩形範囲 (外側スナップ後) を除去する。
     */
    static void deleteRectangle(BufferFacade buffer, Rectangle rect, int tabWidth) {
        for (int li = rect.endLine(); li >= rect.startLine(); li--) {
            var range = cpRangeForLine(buffer, li, rect, tabWidth);
            if (range.rightCp() > range.leftCp()) {
                int lineStart = buffer.lineStartOffset(li);
                buffer.deleteText(lineStart + range.leftCp(), range.rightCp() - range.leftCp());
            }
        }
    }

    /**
     * 矩形の各行を replacement の対応行で置き換える。
     * replacement が矩形の行数より少ない場合、余る行は空文字列で置換される。
     */
    static void replaceRectangle(BufferFacade buffer, Rectangle rect, ListIterable<String> replacement, int tabWidth) {
        int lineCount = rect.lineCount();
        int replacementSize = replacement.size();
        for (int i = lineCount - 1; i >= 0; i--) {
            int li = rect.startLine() + i;
            String rep = i < replacementSize ? replacement.get(i) : "";
            var range = cpRangeForLine(buffer, li, rect, tabWidth);
            int lineStart = buffer.lineStartOffset(li);
            if (range.rightCp() > range.leftCp()) {
                buffer.deleteText(lineStart + range.leftCp(), range.rightCp() - range.leftCp());
            }
            if (!rep.isEmpty()) {
                // 行が leftCol に届かない場合は先に padding してから挿入
                padLineToColumn(buffer, li, rect.leftCol(), tabWidth);
                int refreshedLeftCp = columnRoundDown(buffer.lineText(li), rect.leftCol(), tabWidth);
                buffer.insertText(lineStart + refreshedLeftCp, rep);
            }
        }
    }

    /**
     * 矩形範囲をスペースで埋める（右側テキストは動かさない = 実矩形幅を保持）。
     *
     * <p>跨ぎ文字を含めて削除した後、削除された実カラム幅分のスペースを挿入する。
     * これにより右側テキストの視覚的位置が保たれる。
     */
    static void clearRectangle(BufferFacade buffer, Rectangle rect, int tabWidth) {
        for (int li = rect.endLine(); li >= rect.startLine(); li--) {
            String lineText = buffer.lineText(li);
            int leftCp = columnRoundDown(lineText, rect.leftCol(), tabWidth);
            int rightCp = columnRoundUp(lineText, rect.rightCol(), tabWidth);
            int leftColActual = DisplayWidthUtil.computeColumnForOffset(lineText, leftCp, tabWidth);
            int rightColActual = DisplayWidthUtil.computeColumnForOffset(lineText, rightCp, tabWidth);
            int actualWidth = rightColActual - leftColActual;
            int lineStart = buffer.lineStartOffset(li);
            if (rightCp > leftCp) {
                buffer.deleteText(lineStart + leftCp, rightCp - leftCp);
            }
            // 行が leftCol に届かない場合は末尾 padding してから width 分のスペースを追加
            if (actualWidth == 0) {
                actualWidth = rect.width();
            }
            padLineToColumn(buffer, li, rect.leftCol(), tabWidth);
            int insertCp = columnRoundDown(buffer.lineText(li), rect.leftCol(), tabWidth);
            if (actualWidth > 0) {
                buffer.insertText(lineStart + insertCp, " ".repeat(actualWidth));
            }
        }
    }

    /**
     * 矩形範囲と同サイズの空白を挿入する（右側テキストを右に押し出す）。
     * 挿入位置が跨ぎ文字の中央なら、その文字の右側（外側スナップ右）に挿入する。
     */
    static void openRectangle(BufferFacade buffer, Rectangle rect, int tabWidth) {
        String spaces = " ".repeat(rect.width());
        for (int li = rect.endLine(); li >= rect.startLine(); li--) {
            insertAtColumnRightSnap(buffer, li, rect.leftCol(), spaces, tabWidth);
        }
    }

    /**
     * 現在の point の行を左上として、lines を矩形として挿入する。
     * 挿入位置が跨ぎ文字の中央なら、その文字の右側に挿入する（yank の仕様）。
     * バッファ末尾を超える行は改行を追加してから挿入する。
     */
    static void insertRectangleAtPoint(BufferFacade buffer, int pointOffset, ListIterable<String> lines, int tabWidth) {
        if (lines.isEmpty()) {
            return;
        }
        int startLine = buffer.lineIndexForOffset(pointOffset);
        int startLineStart = buffer.lineStartOffset(startLine);
        String startLineText = buffer.lineText(startLine);
        int cpOffset = pointOffset - startLineStart;
        int startCol = DisplayWidthUtil.computeColumnForOffset(startLineText, cpOffset, tabWidth);

        for (int i = 0; i < lines.size(); i++) {
            int li = startLine + i;
            if (li >= buffer.lineCount()) {
                buffer.insertText(buffer.length(), "\n");
            }
            insertAtColumnRightSnap(buffer, li, startCol, lines.get(i), tabWidth);
        }
    }

    /**
     * 指定行・指定カラムにテキストを挿入する（右スナップ = 跨ぎ文字の後ろに挿入）。
     * 行がそのカラムに届かない場合は末尾にスペース padding してから挿入する。
     */
    static void insertAtColumnRightSnap(BufferFacade buffer, int lineIndex, int col, String text, int tabWidth) {
        padLineToColumn(buffer, lineIndex, col, tabWidth);
        String lineText = buffer.lineText(lineIndex);
        int insertCp = columnRoundUp(lineText, col, tabWidth);
        int lineStart = buffer.lineStartOffset(lineIndex);
        if (!text.isEmpty()) {
            buffer.insertText(lineStart + insertCp, text);
        }
    }

    /**
     * 行の末尾カラムが target 未満なら、target に届くまで末尾にスペースを追加する。
     */
    static void padLineToColumn(BufferFacade buffer, int lineIndex, int targetCol, int tabWidth) {
        String lineText = buffer.lineText(lineIndex);
        int lineLength = codePointCount(lineText);
        int endCol = DisplayWidthUtil.computeColumnForOffset(lineText, lineLength, tabWidth);
        if (endCol < targetCol) {
            int lineStart = buffer.lineStartOffset(lineIndex);
            String padding = " ".repeat(targetCol - endCol);
            buffer.insertText(lineStart + lineLength, padding);
        }
    }

    /**
     * 矩形の左上（startLine, leftCol）に対応するバッファオフセットを返す。
     * leftCol がタブや全角文字の中央に落ちる場合、外側スナップ（手前の文字境界）を採用する。
     * 行が leftCol に届かない場合は行末を返す。
     */
    static int topLeftOffset(BufferFacade buffer, Rectangle rect, int tabWidth) {
        int li = rect.startLine();
        String lineText = buffer.lineText(li);
        int leftCp = columnRoundDown(lineText, rect.leftCol(), tabWidth);
        return buffer.lineStartOffset(li) + leftCp;
    }

    private static int codePointCount(String s) {
        return s.codePointCount(0, s.length());
    }

    private static String codePointSubstring(String s, int startCp, int endCp) {
        int startIdx = s.offsetByCodePoints(0, startCp);
        int endIdx = s.offsetByCodePoints(0, endCp);
        return s.substring(startIdx, endIdx);
    }

    /**
     * 行内のコードポイントオフセット範囲 [leftCp, rightCp)。
     */
    record CpRange(int leftCp, int rightCp) {}
}
