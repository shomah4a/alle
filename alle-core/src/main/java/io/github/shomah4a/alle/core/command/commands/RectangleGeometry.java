package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;

/**
 * 矩形編集のジオメトリ計算ヘルパー。
 *
 * <p>表示カラム ↔ コードポイントオフセット変換、境界が全角文字やタブの中央に落ちるケースで
 * スペース展開した行テキストを構築する処理を提供する。
 *
 * <p>すべての座標系は表示カラム単位（タブは TAB_WIDTH で展開、全角文字は 2）。
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
     * 行テキスト内で指定表示カラムを含む区間のコードポイントオフセットと
     * 区間の左端・右端の実カラムを返す。
     *
     * <p>カラムがタブや全角文字の中央に落ちる場合、その文字全体を覆う区間を返す。
     * 行がカラムに届かない場合、末端位置と行末カラムを返す。
     */
    static CellBoundary cellAtColumn(String lineText, int targetColumn, int tabWidth) {
        int offset = 0;
        int cpIndex = 0;
        int col = 0;
        while (offset < lineText.length()) {
            int codePoint = lineText.codePointAt(offset);
            int width = DisplayWidthUtil.getDisplayWidth(codePoint, col, tabWidth);
            if (col + width > targetColumn) {
                // targetColumn がこの文字に含まれる
                return new CellBoundary(cpIndex, cpIndex + 1, col, col + width);
            }
            col += width;
            offset += Character.charCount(codePoint);
            cpIndex++;
            if (col == targetColumn) {
                // 境界上 → 次の文字の手前
                return new CellBoundary(cpIndex, cpIndex, col, col);
            }
        }
        // 行末を超える: 末端カラムを padding する
        return new CellBoundary(cpIndex, cpIndex, col, col);
    }

    /**
     * 行 lineIndex に対して矩形範囲 [leftCol, rightCol) を切り出すための「展開済み行テキスト」を返す。
     *
     * <p>境界がタブや全角文字の中央に落ちる場合、その文字をスペースに展開する。
     * 行末が leftCol に届かない場合は末尾にスペースを補填する。
     *
     * <p>返り値は展開後の行テキスト本体と、leftCol/rightCol に対応するコードポイントオフセット。
     */
    static PaddedLine paddedLineForRectangle(
            BufferFacade buffer, int lineIndex, int leftCol, int rightCol, int tabWidth) {
        String original = buffer.lineText(lineIndex);
        return buildPaddedLine(original, leftCol, rightCol, tabWidth);
    }

    /**
     * 文字列版。行テキストのみに依存するロジックをテストしやすくするために分離。
     */
    static PaddedLine buildPaddedLine(String lineText, int leftCol, int rightCol, int tabWidth) {
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        int cpIndex = 0;
        int col = 0;
        int leftCp = -1;
        int rightCp = -1;

        while (offset < lineText.length()) {
            int codePoint = lineText.codePointAt(offset);
            int width = DisplayWidthUtil.getDisplayWidth(codePoint, col, tabWidth);
            boolean crossesLeft = col < leftCol && col + width > leftCol;
            boolean crossesRight = col < rightCol && col + width > rightCol;

            if (col == leftCol && leftCp == -1) {
                leftCp = cpIndex;
            }
            if (col == rightCol && rightCp == -1) {
                rightCp = cpIndex;
            }

            if (crossesLeft || crossesRight) {
                // 境界を跨ぐ文字はスペース展開
                for (int i = 0; i < width; i++) {
                    int c = col + i;
                    if (c == leftCol && leftCp == -1) {
                        leftCp = cpIndex;
                    }
                    if (c == rightCol && rightCp == -1) {
                        rightCp = cpIndex;
                    }
                    sb.append(' ');
                    cpIndex++;
                }
                col += width;
                offset += Character.charCount(codePoint);
            } else {
                sb.appendCodePoint(codePoint);
                col += width;
                offset += Character.charCount(codePoint);
                cpIndex++;
            }
        }

        // 行末を越えた境界は末尾スペース padding で埋める
        while (col < leftCol || col < rightCol) {
            if (col == leftCol && leftCp == -1) {
                leftCp = cpIndex;
            }
            if (col == rightCol && rightCp == -1) {
                rightCp = cpIndex;
            }
            if (col >= leftCol && col >= rightCol) {
                break;
            }
            sb.append(' ');
            col++;
            cpIndex++;
        }
        if (leftCp == -1) {
            leftCp = cpIndex;
        }
        if (rightCp == -1) {
            rightCp = cpIndex;
        }

        return new PaddedLine(sb.toString(), leftCp, rightCp);
    }

    /**
     * 指定行の point から挿入するカラム位置でスペース padding し、挿入ポイントのコードポイントオフセットを返す。
     * 行末が目的カラムに届かない場合は末尾にスペースを補填する。
     * 目的カラムがタブや全角文字の中央に落ちる場合はその文字をスペース展開する。
     */
    static PaddedInsertPoint paddedInsertPoint(
            BufferFacade buffer, int lineIndex, int targetColumn, int tabWidth) {
        String original = buffer.lineText(lineIndex);
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        int cpIndex = 0;
        int col = 0;
        while (offset < original.length() && col < targetColumn) {
            int codePoint = original.codePointAt(offset);
            int width = DisplayWidthUtil.getDisplayWidth(codePoint, col, tabWidth);
            if (col + width > targetColumn) {
                for (int i = 0; i < width; i++) {
                    sb.append(' ');
                }
                col += width;
                offset += Character.charCount(codePoint);
                cpIndex += width;
                break;
            }
            sb.appendCodePoint(codePoint);
            col += width;
            offset += Character.charCount(codePoint);
            cpIndex += 1;
        }
        StringBuilder paddingSuffix = new StringBuilder();
        while (col < targetColumn) {
            paddingSuffix.append(' ');
            col++;
        }
        int insertCp = cpIndex + paddingSuffix.length();
        sb.append(paddingSuffix);
        // 残り
        while (offset < original.length()) {
            int codePoint = original.codePointAt(offset);
            sb.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        return new PaddedInsertPoint(sb.toString(), insertCp);
    }

    /**
     * cellAtColumn の戻り値。
     * @param leftCp  含む文字の左境界コードポイントオフセット
     * @param rightCp 含む文字の右境界コードポイントオフセット（leftCp==rightCp なら境界上）
     * @param leftCol 実カラム（leftCp 位置）
     * @param rightCol 実カラム（rightCp 位置）
     */
    record CellBoundary(int leftCp, int rightCp, int leftCol, int rightCol) {}

    /**
     * paddedLineForRectangle / buildPaddedLine の戻り値。
     * text は matrix 展開後の行テキスト。leftCp と rightCp は text 内の
     * 矩形列範囲を示すコードポイントオフセット。
     */
    record PaddedLine(String text, int leftCp, int rightCp) {}

    /**
     * paddedInsertPoint の戻り値。
     * text は padding 後の行テキスト。insertCp は text 内の挿入コードポイントオフセット。
     */
    record PaddedInsertPoint(String text, int insertCp) {}
}
