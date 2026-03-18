package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.highlight.StyledSpan;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Rect;
import io.github.shomah4a.alle.core.window.Separator;
import io.github.shomah4a.alle.core.window.WindowLayout;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * Frame/Window/Bufferの内容をLanternaのScreenに描画する。
 * createSnapshotでFrameの状態をimmutableなスナップショットに変換し、
 * renderSnapshotでスナップショットを画面に描画する。
 *
 * 画面レイアウト:
 * - 行0 〜 rows-2: ウィンドウツリー領域（各ウィンドウにバッファ表示+モードライン）
 * - 行rows-1: ミニバッファ
 */
public class ScreenRenderer {

    private final Screen screen;
    private final FaceResolver faceResolver;
    private final MessageBuffer messageBuffer;

    public ScreenRenderer(Screen screen, MessageBuffer messageBuffer) {
        this.screen = screen;
        this.faceResolver = new FaceResolver();
        this.messageBuffer = messageBuffer;
    }

    /**
     * フレームの内容を画面に描画する。
     */
    public void render(Frame frame) throws IOException {
        TerminalSize size = screen.getTerminalSize();
        if (size.getRows() < 3) {
            screen.refresh(Screen.RefreshType.DELTA);
            return;
        }

        var snapshot = createSnapshot(frame, size);
        renderSnapshot(snapshot);
        screen.refresh(Screen.RefreshType.DELTA);
    }

    /**
     * Frameの状態からimmutableな描画スナップショットを作成する。
     * ensurePointVisible等の副作用を伴う調整もここで行う。
     */
    RenderSnapshot createSnapshot(Frame frame, TerminalSize size) {
        int cols = size.getColumns();
        int rows = size.getRows();
        int windowAreaRows = rows - 1;
        int minibufferRow = rows - 1;

        var treeArea = new Rect(0, 0, cols, windowAreaRows);
        var layoutResult = WindowLayout.compute(frame.getWindowTree(), treeArea);

        var windowSnapshots = Lists.mutable.<RenderSnapshot.WindowSnapshot>empty();

        layoutResult.windowRects().forEachKeyValue((window, rect) -> {
            if (rect.height() < 2) {
                return;
            }
            int bufferRows = rect.height() - 1;
            window.ensurePointVisible(bufferRows);
            window.ensurePointHorizontallyVisible(rect.width());

            var buffer = window.getBuffer();
            int lineCount = buffer.lineCount();
            int displayStart = window.getDisplayStartLine();
            int displayStartColumn = window.getDisplayStartColumn();
            var highlighter = buffer.getMajorMode().highlighter();

            var visibleLines = Lists.mutable.<RenderSnapshot.LineSnapshot>empty();
            for (int row = 0; row < bufferRows; row++) {
                int lineIndex = displayStart + row;
                if (lineIndex < lineCount) {
                    String lineText = buffer.lineText(lineIndex);
                    Optional<ListIterable<StyledSpan>> spans = highlighter.map(h -> h.highlight(lineText));
                    visibleLines.add(new RenderSnapshot.LineSnapshot(lineText, spans));
                }
            }

            String modeLine = buildModeLineText(window, rect.width());
            windowSnapshots.add(new RenderSnapshot.WindowSnapshot(rect, visibleLines, displayStartColumn, modeLine));
        });

        // ミニバッファ / エコーエリア
        RenderSnapshot.MinibufferSnapshot minibufferSnapshot;
        if (frame.isMinibufferActive()) {
            var minibufferWindow = frame.getMinibufferWindow();
            minibufferWindow.ensurePointHorizontallyVisible(cols);
            var buffer = minibufferWindow.getBuffer();
            String text = buffer.length() > 0 ? buffer.getText() : "";
            minibufferSnapshot =
                    new RenderSnapshot.MinibufferSnapshot(Optional.of(text), minibufferWindow.getDisplayStartColumn());
        } else if (messageBuffer.isShowingMessage()) {
            var msg = messageBuffer.getLastMessage();
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(msg, 0);
        } else {
            minibufferSnapshot = new RenderSnapshot.MinibufferSnapshot(Optional.empty(), 0);
        }

        // カーソル位置
        TerminalPosition cursorPosition;
        if (frame.isMinibufferActive()) {
            cursorPosition = computeMinibufferCursorPosition(frame.getMinibufferWindow(), minibufferRow, cols);
        } else {
            var activeWindow = frame.getActiveWindow();
            var activeRect = layoutResult.windowRects().get(activeWindow);
            if (activeRect != null && activeRect.height() >= 2) {
                cursorPosition = computeCursorPosition(activeWindow, activeRect);
            } else {
                cursorPosition = new TerminalPosition(0, 0);
            }
        }

        return new RenderSnapshot(
                cols, rows, windowSnapshots, layoutResult.separators(), minibufferSnapshot, cursorPosition);
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
                        sep.column(), row, new TextCharacter('│', TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT));
            }
        }

        // ミニバッファ行
        var mb = snapshot.minibuffer();
        if (mb.text().isPresent()) {
            renderLineAt(mb.text().get(), minibufferRow, 0, cols, mb.displayStartColumn());
        } else {
            fillBlank(minibufferRow, 0, cols);
        }

        // カーソル位置
        screen.setCursorPosition(snapshot.cursorPosition());
    }

    private void renderWindowSnapshot(RenderSnapshot.WindowSnapshot ws) {
        var rect = ws.rect();
        int bufferRows = rect.height() - 1;
        int displayStartColumn = ws.displayStartColumn();

        for (int row = 0; row < ws.visibleLines().size(); row++) {
            var line = ws.visibleLines().get(row);
            int screenRow = rect.top() + row;
            if (line.spans().isPresent()) {
                renderLineWithHighlight(
                        line.text(),
                        screenRow,
                        rect.left(),
                        rect.width(),
                        displayStartColumn,
                        line.spans().get());
            } else {
                renderLineAt(line.text(), screenRow, rect.left(), rect.width(), displayStartColumn);
            }
        }

        // 空行の余白は既にウィンドウ領域全体の空白初期化でカバー済み

        // モードライン
        renderModeLine(ws.modeLine(), rect.top() + rect.height() - 1, rect.left(), rect.width());
    }

    private String buildModeLineText(io.github.shomah4a.alle.core.window.Window window, int maxColumns) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int column = point - lineStart;

        String dirty = buffer.isDirty() ? "**" : "--";
        String bufferName = buffer.getName();
        String modeName = buffer.getMajorMode().name();
        return String.format("--%s  %s    (%d,%d)  (%s)", dirty, bufferName, lineIndex + 1, column, modeName);
    }

    private void renderModeLine(String modeLine, int row, int left, int maxColumns) {
        for (int col = 0; col < maxColumns; col++) {
            char ch = col < modeLine.length() ? modeLine.charAt(col) : '-';
            screen.setCharacter(
                    left + col,
                    row,
                    new TextCharacter(ch, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT, SGR.REVERSE));
        }
    }

    private TerminalPosition computeCursorPosition(io.github.shomah4a.alle.core.window.Window window, Rect rect) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int displayStart = window.getDisplayStartLine();
        int bufferRows = rect.height() - 1;

        int screenRow = lineIndex - displayStart;
        if (screenRow < 0 || screenRow >= bufferRows) {
            return new TerminalPosition(rect.left(), rect.top());
        }

        int col = DisplayWidthUtil.computeColumnForOffset(buffer.lineText(lineIndex), point - lineStart);
        int screenCol = col - window.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < rect.width()) {
            return new TerminalPosition(rect.left() + screenCol, rect.top() + screenRow);
        }
        return new TerminalPosition(rect.left(), rect.top());
    }

    private TerminalPosition computeMinibufferCursorPosition(
            io.github.shomah4a.alle.core.window.Window minibufferWindow, int row, int maxColumns) {
        var buffer = minibufferWindow.getBuffer();
        int point = minibufferWindow.getPoint();
        String text = buffer.getText();
        int col = DisplayWidthUtil.computeColumnForOffset(text, point);
        int screenCol = col - minibufferWindow.getDisplayStartColumn();
        if (screenCol >= 0 && screenCol < maxColumns) {
            return new TerminalPosition(screenCol, row);
        }
        return new TerminalPosition(0, row);
    }

    private void renderLineWithHighlight(
            String text, int row, int left, int maxColumns, int displayStartColumn, ListIterable<StyledSpan> spans) {
        int textCol = 0;
        int charOffset = 0;
        int cpIndex = 0;
        int spanIdx = 0;

        // displayStartColumn までスキップ
        while (charOffset < text.length() && textCol < displayStartColumn) {
            int codePoint = text.codePointAt(charOffset);
            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint);
            textCol += displayWidth;
            charOffset += Character.charCount(codePoint);
            cpIndex++;
        }

        // displayStartColumn が全角文字の途中だった場合、1カラム空白を埋める
        int screenCol = 0;
        if (textCol > displayStartColumn) {
            screen.setCharacter(left, row, TextCharacter.fromString(" ")[0]);
            screenCol = 1;
        }

        // 描画
        while (charOffset < text.length() && screenCol < maxColumns) {
            int codePoint = text.codePointAt(charOffset);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(charOffset, charOffset + charCount);

            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint);
            if (screenCol + displayWidth > maxColumns) {
                break;
            }

            // 現在のコードポイント位置に適用するスパンを探す
            while (spanIdx < spans.size() && spans.get(spanIdx).end() <= cpIndex) {
                spanIdx++;
            }

            TextCharacter tc;
            if (spanIdx < spans.size()
                    && spans.get(spanIdx).start() <= cpIndex
                    && cpIndex < spans.get(spanIdx).end()) {
                var resolved = faceResolver.resolve(spans.get(spanIdx).face());
                tc = TextCharacter.fromString(ch, resolved.foreground(), resolved.background(), resolved.sgrs())[0];
            } else {
                tc = TextCharacter.fromString(ch)[0];
            }

            screen.setCharacter(left + screenCol, row, tc);
            screenCol += displayWidth;
            charOffset += charCount;
            cpIndex++;
        }

        // 行末の余白を空白で埋める
        fillBlank(row, left + screenCol, left + maxColumns);
    }

    private void renderLineAt(String text, int row, int left, int maxColumns, int displayStartColumn) {
        int textCol = 0;
        int offset = 0;

        // displayStartColumn までスキップ
        while (offset < text.length() && textCol < displayStartColumn) {
            int codePoint = text.codePointAt(offset);
            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint);
            textCol += displayWidth;
            offset += Character.charCount(codePoint);
        }

        // displayStartColumn が全角文字の途中だった場合、1カラム空白を埋める
        int screenCol = 0;
        if (textCol > displayStartColumn) {
            screen.setCharacter(left, row, TextCharacter.fromString(" ")[0]);
            screenCol = 1;
        }

        // 描画
        while (offset < text.length() && screenCol < maxColumns) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(offset, offset + charCount);

            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint);
            if (screenCol + displayWidth > maxColumns) {
                break;
            }

            screen.setCharacter(left + screenCol, row, TextCharacter.fromString(ch)[0]);

            screenCol += displayWidth;
            offset += charCount;
        }

        // 行末の余白を空白で埋める
        fillBlank(row, left + screenCol, left + maxColumns);
    }

    private void fillBlank(int row, int fromCol, int toCol) {
        var blank = TextCharacter.fromString(" ")[0];
        for (int col = fromCol; col < toCol; col++) {
            screen.setCharacter(col, row, blank);
        }
    }
}
