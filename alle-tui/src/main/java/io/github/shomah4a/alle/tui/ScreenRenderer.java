package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowTree;
import java.io.IOException;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Frame/Window/Bufferの内容をLanternaのScreenに描画する。
 * 画面レイアウト:
 * - 行0 〜 rows-3: バッファ表示エリア
 * - 行rows-2: モードライン（反転表示）
 * - 行rows-1: ミニバッファ
 */
public class ScreenRenderer {

    private final Screen screen;

    public ScreenRenderer(Screen screen) {
        this.screen = screen;
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

        int bufferAreaRows = rows - 2;
        int modeLineRow = rows - 2;
        int minibufferRow = rows - 1;

        // メインウィンドウ（ウィンドウツリーの最初のウィンドウ）を取得
        Window mainWindow = findFirstWindow(frame.getWindowTree());

        // バッファ表示
        renderWindow(mainWindow, bufferAreaRows, cols);

        // モードライン
        renderModeLine(mainWindow, modeLineRow, cols);

        // ミニバッファ
        renderMinibuffer(frame.getMinibufferWindow(), minibufferRow, cols);

        // カーソル位置
        if (frame.isMinibufferActive()) {
            positionMinibufferCursor(frame.getMinibufferWindow(), minibufferRow, cols);
        } else {
            positionCursor(mainWindow, bufferAreaRows);
        }

        screen.refresh();
    }

    private void renderWindow(Window window, int maxRows, int maxColumns) {
        var buffer = window.getBuffer();
        int lineCount = buffer.lineCount();
        int displayStart = window.getDisplayStartLine();

        for (int row = 0; row < maxRows && displayStart + row < lineCount; row++) {
            int lineIndex = displayStart + row;
            String lineText = buffer.lineText(lineIndex);
            renderLine(lineText, row, maxColumns);
        }
    }

    private void renderModeLine(Window window, int row, int maxColumns) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int column = point - lineStart;

        String dirty = buffer.isDirty() ? "**" : "--";
        String bufferName = buffer.getName();
        String modeLine = String.format("--%s  %s    (%d,%d)", dirty, bufferName, lineIndex + 1, column);

        // モードライン全体を反転表示で描画
        for (int col = 0; col < maxColumns; col++) {
            char ch = col < modeLine.length() ? modeLine.charAt(col) : '-';
            screen.setCharacter(
                    col, row, new TextCharacter(ch, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT, SGR.REVERSE));
        }
    }

    private void renderMinibuffer(Window minibufferWindow, int row, int maxColumns) {
        var buffer = minibufferWindow.getBuffer();
        if (buffer.length() == 0) {
            return;
        }
        String text = buffer.getText();
        renderLineAt(text, row, maxColumns);
    }

    private void renderLine(String lineText, int row, int maxColumns) {
        renderLineAt(lineText, row, maxColumns);
    }

    private void renderLineAt(String text, int row, int maxColumns) {
        int col = 0;
        int offset = 0;
        while (offset < text.length() && col < maxColumns) {
            int codePoint = text.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(offset, offset + charCount);

            int displayWidth = getDisplayWidth(codePoint);
            if (col + displayWidth > maxColumns) {
                break;
            }

            screen.setCharacter(col, row, TextCharacter.fromString(ch)[0]);

            col += displayWidth;
            offset += charCount;
        }
    }

    private void positionCursor(Window window, int maxRows) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int displayStart = window.getDisplayStartLine();

        int screenRow = lineIndex - displayStart;
        if (screenRow < 0 || screenRow >= maxRows) {
            return;
        }

        int col = computeColumnForOffset(buffer.lineText(lineIndex), point - lineStart);
        screen.setCursorPosition(new TerminalPosition(col, screenRow));
    }

    private void positionMinibufferCursor(Window minibufferWindow, int row, int maxColumns) {
        var buffer = minibufferWindow.getBuffer();
        int point = minibufferWindow.getPoint();
        String text = buffer.getText();
        int col = computeColumnForOffset(text, point);
        if (col < maxColumns) {
            screen.setCursorPosition(new TerminalPosition(col, row));
        }
    }

    /**
     * 行内のコードポイントオフセットから画面上のカラム位置を計算する。
     */
    private static int computeColumnForOffset(String lineText, int codePointOffset) {
        int col = 0;
        int offset = 0;
        int cpCount = 0;
        while (offset < lineText.length() && cpCount < codePointOffset) {
            int codePoint = lineText.codePointAt(offset);
            col += getDisplayWidth(codePoint);
            offset += Character.charCount(codePoint);
            cpCount++;
        }
        return col;
    }

    /**
     * コードポイントの表示幅を返す。
     * CJK文字等の全角文字は2、それ以外は1。
     */
    private static int getDisplayWidth(int codePoint) {
        if (isFullWidth(codePoint)) {
            return 2;
        }
        return 1;
    }

    /**
     * 全角文字かどうかを判定する。
     * East Asian Width が Wide (W) または Fullwidth (F) の場合にtrueを返す。
     */
    private static final ImmutableSet<Character.UnicodeBlock> FULL_WIDTH_BLOCKS = Sets.immutable.with(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
            Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
            Character.UnicodeBlock.HIRAGANA,
            Character.UnicodeBlock.KATAKANA,
            Character.UnicodeBlock.HANGUL_SYLLABLES,
            Character.UnicodeBlock.EMOTICONS,
            Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS);

    private static boolean isFullWidth(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        if (block == null) {
            return false;
        }
        if (FULL_WIDTH_BLOCKS.contains(block)) {
            return true;
        }
        // HALFWIDTH_AND_FULLWIDTH_FORMSは半角文字も含むため、全角範囲のみ判定
        if (block.equals(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS)) {
            return (codePoint >= 0xFF01 && codePoint <= 0xFF60) || (codePoint >= 0xFFE0 && codePoint <= 0xFFE6);
        }
        return false;
    }

    private static Window findFirstWindow(WindowTree tree) {
        return switch (tree) {
            case WindowTree.Leaf leaf -> leaf.window();
            case WindowTree.Split split -> findFirstWindow(split.first());
        };
    }
}
