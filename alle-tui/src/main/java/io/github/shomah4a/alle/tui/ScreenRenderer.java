package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.DisplayWidthUtil;
import io.github.shomah4a.alle.core.highlight.StyledSpan;
import io.github.shomah4a.alle.core.highlight.SyntaxHighlighter;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.LayoutResult;
import io.github.shomah4a.alle.core.window.Rect;
import io.github.shomah4a.alle.core.window.Separator;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowLayout;
import java.io.IOException;
import java.util.Optional;
import org.eclipse.collections.api.list.ListIterable;

/**
 * Frame/Window/Bufferの内容をLanternaのScreenに描画する。
 * 画面レイアウト:
 * - 行0 〜 rows-2: ウィンドウツリー領域（各ウィンドウにバッファ表示+モードライン）
 * - 行rows-1: ミニバッファ
 */
public class ScreenRenderer {

    private final Screen screen;
    private final FaceResolver faceResolver;

    public ScreenRenderer(Screen screen) {
        this.screen = screen;
        this.faceResolver = new FaceResolver();
    }

    /**
     * フレームの内容を画面に描画する。
     */
    public void render(Frame frame) throws IOException {
        screen.clear();
        TerminalSize size = screen.getTerminalSize();
        int cols = size.getColumns();
        int rows = size.getRows();

        if (rows < 3) {
            screen.refresh();
            return;
        }

        int windowAreaRows = rows - 1;
        int minibufferRow = rows - 1;

        // レイアウト計算
        var treeArea = new Rect(0, 0, cols, windowAreaRows);
        LayoutResult layoutResult = WindowLayout.compute(frame.getWindowTree(), treeArea);

        // 各ウィンドウを描画
        layoutResult.windowRects().forEachKeyValue((window, rect) -> {
            if (rect.height() < 2) {
                return;
            }
            int bufferRows = rect.height() - 1;
            window.ensurePointVisible(bufferRows);
            renderWindowInRect(window, rect);
        });

        // 垂直分割セパレータ描画
        for (Separator sep : layoutResult.separators()) {
            for (int row = sep.top(); row < sep.top() + sep.height(); row++) {
                screen.setCharacter(
                        sep.column(), row, new TextCharacter('│', TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT));
            }
        }

        // ミニバッファ
        renderMinibuffer(frame.getMinibufferWindow(), minibufferRow, cols);

        // カーソル位置
        if (frame.isMinibufferActive()) {
            positionMinibufferCursor(frame.getMinibufferWindow(), minibufferRow, cols);
        } else {
            var activeWindow = frame.getActiveWindow();
            var activeRect = layoutResult.windowRects().get(activeWindow);
            if (activeRect != null && activeRect.height() >= 2) {
                positionCursorInRect(activeWindow, activeRect);
            }
        }

        screen.refresh();
    }

    private void renderWindowInRect(Window window, Rect rect) {
        var buffer = window.getBuffer();
        int lineCount = buffer.lineCount();
        int displayStart = window.getDisplayStartLine();
        int bufferRows = rect.height() - 1; // 最終行はモードライン
        Optional<SyntaxHighlighter> highlighter = buffer.getMajorMode().highlighter();

        for (int row = 0; row < bufferRows && displayStart + row < lineCount; row++) {
            int lineIndex = displayStart + row;
            String lineText = buffer.lineText(lineIndex);
            int screenRow = rect.top() + row;
            if (highlighter.isPresent()) {
                var spans = highlighter.get().highlight(lineText);
                renderLineWithHighlight(lineText, screenRow, rect.left(), rect.width(), spans);
            } else {
                renderLineAt(lineText, screenRow, rect.left(), rect.width());
            }
        }

        // モードライン
        renderModeLine(window, rect.top() + rect.height() - 1, rect.left(), rect.width());
    }

    private void renderModeLine(Window window, int row, int left, int maxColumns) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int column = point - lineStart;

        String dirty = buffer.isDirty() ? "**" : "--";
        String bufferName = buffer.getName();
        String modeName = buffer.getMajorMode().name();
        String modeLine =
                String.format("--%s  %s    (%d,%d)  (%s)", dirty, bufferName, lineIndex + 1, column, modeName);

        // モードライン全体を反転表示で描画
        for (int col = 0; col < maxColumns; col++) {
            char ch = col < modeLine.length() ? modeLine.charAt(col) : '-';
            screen.setCharacter(
                    left + col,
                    row,
                    new TextCharacter(ch, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT, SGR.REVERSE));
        }
    }

    private void renderMinibuffer(Window minibufferWindow, int row, int maxColumns) {
        var buffer = minibufferWindow.getBuffer();
        if (buffer.length() == 0) {
            return;
        }
        String text = buffer.getText();
        renderLineAt(text, row, 0, maxColumns);
    }

    private void renderLineWithHighlight(
            String text, int row, int left, int maxColumns, ListIterable<StyledSpan> spans) {
        int col = 0;
        int charOffset = 0;
        int cpIndex = 0;
        int spanIdx = 0;

        while (charOffset < text.length() && col < maxColumns) {
            int codePoint = text.codePointAt(charOffset);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(charOffset, charOffset + charCount);

            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint);
            if (col + displayWidth > maxColumns) {
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

            screen.setCharacter(left + col, row, tc);
            col += displayWidth;
            charOffset += charCount;
            cpIndex++;
        }
    }

    private void renderLineAt(String text, int row, int left, int maxColumns) {
        int col = 0;
        int offset = 0;
        while (offset < text.length() && col < maxColumns) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(offset, offset + charCount);

            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint);
            if (col + displayWidth > maxColumns) {
                break;
            }

            screen.setCharacter(left + col, row, TextCharacter.fromString(ch)[0]);

            col += displayWidth;
            offset += charCount;
        }
    }

    private void positionCursorInRect(Window window, Rect rect) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int displayStart = window.getDisplayStartLine();
        int bufferRows = rect.height() - 1;

        int screenRow = lineIndex - displayStart;
        if (screenRow < 0 || screenRow >= bufferRows) {
            return;
        }

        int col = DisplayWidthUtil.computeColumnForOffset(buffer.lineText(lineIndex), point - lineStart);
        screen.setCursorPosition(new TerminalPosition(rect.left() + col, rect.top() + screenRow));
    }

    private void positionMinibufferCursor(Window minibufferWindow, int row, int maxColumns) {
        var buffer = minibufferWindow.getBuffer();
        int point = minibufferWindow.getPoint();
        String text = buffer.getText();
        int col = DisplayWidthUtil.computeColumnForOffset(text, point);
        if (col < maxColumns) {
            screen.setCursorPosition(new TerminalPosition(col, row));
        }
    }
}
