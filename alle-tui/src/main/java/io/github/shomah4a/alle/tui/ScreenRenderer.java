package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.render.RenderSnapshot;
import io.github.shomah4a.alle.core.styling.FaceTheme;
import io.github.shomah4a.alle.core.styling.StyledSpan;
import io.github.shomah4a.alle.core.window.Separator;
import org.eclipse.collections.api.list.ListIterable;

/**
 * RenderSnapshotをLanternaのScreenに描画する。
 *
 * <p>画面レイアウト:
 * <ul>
 *   <li>行0 〜 rows-2: ウィンドウツリー領域（各ウィンドウにバッファ表示+モードライン）</li>
 *   <li>行rows-1: ミニバッファ</li>
 * </ul>
 */
public class ScreenRenderer {

    private final Screen screen;
    private final FaceTheme faceTheme;
    private final FaceResolver faceResolver;

    public ScreenRenderer(Screen screen, FaceTheme faceTheme, FaceResolver faceResolver) {
        this.screen = screen;
        this.faceTheme = faceTheme;
        this.faceResolver = faceResolver;
    }

    /**
     * スナップショットを画面に描画する。screen.clear()は呼ばない。
     */
    void renderSnapshot(RenderSnapshot snapshot) {
        int cols = snapshot.screenCols();
        int rows = snapshot.screenRows();
        int windowAreaRows = rows - 1;
        int minibufferRow = rows - 1;

        // ウィンドウ領域全体を空白で初期化
        for (int row = 0; row < windowAreaRows; row++) {
            fillBlank(row, 0, cols);
        }

        // 各ウィンドウを描画
        for (var ws : snapshot.windowSnapshots()) {
            renderWindowSnapshot(ws);
        }

        // 垂直分割セパレータ描画
        for (Separator sep : snapshot.separators()) {
            for (int row = sep.top(); row < sep.top() + sep.height(); row++) {
                screen.setCharacter(
                        sep.column(),
                        row,
                        TextCharacter.fromCharacter('│', TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT)[0]);
            }
        }

        // ミニバッファ行
        var mb = snapshot.minibuffer();
        if (mb.text().isPresent()) {
            if (mb.spans().isPresent()) {
                renderLineWithHighlight(
                        mb.text().get(),
                        minibufferRow,
                        0,
                        cols,
                        mb.displayStartColumn(),
                        mb.tabWidth(),
                        0,
                        Integer.MAX_VALUE,
                        mb.spans().get());
            } else {
                renderLineAt(
                        mb.text().get(),
                        minibufferRow,
                        0,
                        cols,
                        mb.displayStartColumn(),
                        mb.tabWidth(),
                        0,
                        Integer.MAX_VALUE);
            }
        } else {
            fillBlank(minibufferRow, 0, cols);
        }

        // カーソル位置
        var cursor = snapshot.cursorPosition();
        screen.setCursorPosition(new TerminalPosition(cursor.column(), cursor.row()));
    }

    private void renderWindowSnapshot(RenderSnapshot.WindowSnapshot ws) {
        var rect = ws.rect();
        int displayStartColumn = ws.displayStartColumn();
        int tabWidth = ws.tabWidth();
        var highlightLine = ws.highlightLine();

        for (int row = 0; row < ws.visibleLines().size(); row++) {
            var line = ws.visibleLines().get(row);
            int screenRow = rect.top() + row;
            boolean isHighlighted = highlightLine.isPresent() && highlightLine.getAsInt() == row;

            // 折り返しモードでは startCp から描画開始（視覚行ローカル基準）、
            // 切り詰めモードでは startCp=0 + displayStartColumn スキップ（行頭基準）
            int startCp = 0;
            int maxCpIndex = Integer.MAX_VALUE;
            int effectiveDisplayStartColumn = displayStartColumn;
            if (line.visualLineRange().isPresent()) {
                var vlr = line.visualLineRange().get();
                startCp = vlr.startCp();
                maxCpIndex = vlr.endCp();
                effectiveDisplayStartColumn = 0;
            }

            if (line.spans().isPresent()) {
                renderLineWithHighlight(
                        line.text(),
                        screenRow,
                        rect.left(),
                        rect.width(),
                        effectiveDisplayStartColumn,
                        tabWidth,
                        startCp,
                        maxCpIndex,
                        line.spans().get());
            } else {
                renderLineAt(
                        line.text(),
                        screenRow,
                        rect.left(),
                        rect.width(),
                        effectiveDisplayStartColumn,
                        tabWidth,
                        startCp,
                        maxCpIndex);
            }
            if (isHighlighted) {
                applyReverse(screenRow, rect.left(), rect.width());
            }
        }

        // リージョンハイライト
        if (ws.regionRange().isPresent()) {
            applyRegionHighlight(ws);
        }

        // 空行の余白は既にウィンドウ領域全体の空白初期化でカバー済み

        // モードライン
        renderModeLine(ws.modeLine(), rect.top() + rect.height() - 1, rect.left(), rect.width());
    }

    private void renderModeLine(String modeLine, int row, int left, int maxColumns) {
        for (int col = 0; col < maxColumns; col++) {
            char ch = col < modeLine.length() ? modeLine.charAt(col) : '-';
            screen.setCharacter(
                    left + col,
                    row,
                    TextCharacter.fromCharacter(ch, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT, SGR.REVERSE)[0]);
        }
    }

    private void renderLineWithHighlight(
            String text,
            int row,
            int left,
            int maxColumns,
            int displayStartColumn,
            int tabWidth,
            int startCp,
            int maxCpIndex,
            ListIterable<StyledSpan> spans) {
        int charOffset = 0;
        int cpIndex = 0;
        int spanIdx = 0;

        // startCp までスキップ（折り返しモードの視覚行開始位置まで）
        while (charOffset < text.length() && cpIndex < startCp) {
            int codePoint = text.codePointAt(charOffset);
            charOffset += Character.charCount(codePoint);
            cpIndex++;
        }

        // ここから textCol を 0 ベースで再計算（視覚行ローカル基準）
        int textCol = 0;

        // displayStartColumn までスキップ（切り詰めモードの水平スクロール分）
        while (charOffset < text.length() && textCol < displayStartColumn && cpIndex < maxCpIndex) {
            int codePoint = text.codePointAt(charOffset);
            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint, textCol, tabWidth);
            textCol += displayWidth;
            charOffset += Character.charCount(codePoint);
            cpIndex++;
        }

        // displayStartColumn が文字の途中だった場合、はみ出した分を空白で埋める
        int screenCol = 0;
        if (textCol > displayStartColumn) {
            int pad = Math.min(textCol - displayStartColumn, maxColumns);
            for (int i = 0; i < pad; i++) {
                screen.setCharacter(left + i, row, TextCharacter.fromString(" ")[0]);
            }
            screenCol = pad;
        }

        // 描画
        while (charOffset < text.length() && screenCol < maxColumns && cpIndex < maxCpIndex) {
            int codePoint = text.codePointAt(charOffset);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(charOffset, charOffset + charCount);

            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint, textCol, tabWidth);
            if (screenCol + displayWidth > maxColumns) {
                break;
            }

            // 現在のコードポイント位置に適用するスパンを探す
            while (spanIdx < spans.size() && spans.get(spanIdx).end() <= cpIndex) {
                spanIdx++;
            }

            TextColor fg = null;
            TextColor bg = null;
            SGR[] sgrs = null;
            boolean styled = false;
            if (spanIdx < spans.size()
                    && spans.get(spanIdx).start() <= cpIndex
                    && cpIndex < spans.get(spanIdx).end()) {
                var faceSpec = faceTheme.resolve(spans.get(spanIdx).faceName());
                var resolved = faceResolver.resolve(faceSpec);
                fg = resolved.foreground();
                bg = resolved.background();
                sgrs = resolved.sgrs().toArray(new SGR[0]);
                styled = true;
            }

            if (codePoint == '\t') {
                // タブは displayWidth 個の空白で展開
                for (int i = 0; i < displayWidth && screenCol + i < maxColumns; i++) {
                    TextCharacter blank =
                            styled ? TextCharacter.fromString(" ", fg, bg, sgrs)[0] : TextCharacter.fromString(" ")[0];
                    screen.setCharacter(left + screenCol + i, row, blank);
                }
            } else {
                TextCharacter tc =
                        styled ? TextCharacter.fromString(ch, fg, bg, sgrs)[0] : TextCharacter.fromString(ch)[0];
                screen.setCharacter(left + screenCol, row, tc);
            }

            screenCol += displayWidth;
            textCol += displayWidth;
            charOffset += charCount;
            cpIndex++;
        }

        // 行末の余白を空白で埋める
        fillBlank(row, left + screenCol, left + maxColumns);
    }

    private void renderLineAt(
            String text,
            int row,
            int left,
            int maxColumns,
            int displayStartColumn,
            int tabWidth,
            int startCp,
            int maxCpIndex) {
        int offset = 0;
        int cpIndex = 0;

        // startCp までスキップ（折り返しモードの視覚行開始位置まで）
        while (offset < text.length() && cpIndex < startCp) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            cpIndex++;
        }

        // ここから textCol を 0 ベースで再計算（視覚行ローカル基準）
        int textCol = 0;

        // displayStartColumn までスキップ（切り詰めモードの水平スクロール分）
        while (offset < text.length() && textCol < displayStartColumn && cpIndex < maxCpIndex) {
            int codePoint = text.codePointAt(offset);
            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint, textCol, tabWidth);
            textCol += displayWidth;
            offset += Character.charCount(codePoint);
            cpIndex++;
        }

        // displayStartColumn が文字の途中だった場合、はみ出した分を空白で埋める
        int screenCol = 0;
        if (textCol > displayStartColumn) {
            int pad = Math.min(textCol - displayStartColumn, maxColumns);
            for (int i = 0; i < pad; i++) {
                screen.setCharacter(left + i, row, TextCharacter.fromString(" ")[0]);
            }
            screenCol = pad;
        }

        // 描画
        while (offset < text.length() && screenCol < maxColumns && cpIndex < maxCpIndex) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(offset, offset + charCount);

            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint, textCol, tabWidth);
            if (screenCol + displayWidth > maxColumns) {
                break;
            }

            if (codePoint == '\t') {
                TextCharacter blank = TextCharacter.fromString(" ")[0];
                for (int i = 0; i < displayWidth && screenCol + i < maxColumns; i++) {
                    screen.setCharacter(left + screenCol + i, row, blank);
                }
            } else {
                screen.setCharacter(left + screenCol, row, TextCharacter.fromString(ch)[0]);
            }

            screenCol += displayWidth;
            textCol += displayWidth;
            offset += charCount;
            cpIndex++;
        }

        // 行末の余白を空白で埋める
        fillBlank(row, left + screenCol, left + maxColumns);
    }

    private void applyRegionHighlight(RenderSnapshot.WindowSnapshot ws) {
        var rect = ws.rect();
        int displayStartColumn = ws.displayStartColumn();
        int tabWidth = ws.tabWidth();
        for (int row = 0; row < ws.visibleLines().size(); row++) {
            var line = ws.visibleLines().get(row);
            var lineRegion = line.regionInLine();
            if (lineRegion.isEmpty()) {
                continue;
            }
            int startCp = lineRegion.get().startCp();
            int endCp = lineRegion.get().endCp();
            int screenRow = rect.top() + row;

            int screenStartCol;
            int screenEndCol;
            if (line.visualLineRange().isPresent()) {
                // 折り返しモード: 視覚行ローカル基準でカラムを計算
                var vlr = line.visualLineRange().get();
                startCp = Math.max(startCp, vlr.startCp());
                endCp = Math.min(endCp, vlr.endCp());
                if (startCp >= endCp) {
                    continue;
                }
                int startCol =
                        DisplayWidthUtil.computeColumnWidthInRange(line.text(), vlr.startCp(), startCp, tabWidth);
                int endCol = DisplayWidthUtil.computeColumnWidthInRange(line.text(), vlr.startCp(), endCp, tabWidth);
                screenStartCol = Math.max(0, startCol);
                screenEndCol = Math.min(rect.width(), endCol);
            } else {
                // 切り詰めモード: 行頭基準で displayStartColumn を引く
                int startCol = DisplayWidthUtil.computeColumnForOffset(line.text(), startCp, tabWidth);
                int endCol = DisplayWidthUtil.computeColumnForOffset(line.text(), endCp, tabWidth);
                screenStartCol = Math.max(0, startCol - displayStartColumn);
                screenEndCol = Math.min(rect.width(), endCol - displayStartColumn);
            }

            if (screenStartCol >= screenEndCol) {
                continue;
            }

            applyReverse(screenRow, rect.left() + screenStartCol, screenEndCol - screenStartCol);
        }
    }

    private void applyReverse(int row, int left, int width) {
        for (int col = left; col < left + width; col++) {
            var tc = screen.getBackCharacter(new TerminalPosition(col, row));
            screen.setCharacter(col, row, tc.withModifier(SGR.REVERSE));
        }
    }

    private void fillBlank(int row, int fromCol, int toCol) {
        var blank = TextCharacter.fromString(" ")[0];
        for (int col = fromCol; col < toCol; col++) {
            screen.setCharacter(col, row, blank);
        }
    }
}
