package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.IOException;
import java.util.Set;

/**
 * Frame/Window/Bufferの内容をLanternaのScreenに描画する。
 * 最小限の実装: アクティブウィンドウのバッファ内容とカーソル位置のみ。
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
        Window window = frame.getActiveWindow();

        renderWindow(window, size);
        positionCursor(window, size);

        screen.refresh();
    }

    private void renderWindow(Window window, TerminalSize size) {
        var buffer = window.getBuffer();
        int lineCount = buffer.lineCount();
        int displayStart = window.getDisplayStartLine();
        int rows = size.getRows();

        for (int row = 0; row < rows && displayStart + row < lineCount; row++) {
            int lineIndex = displayStart + row;
            String lineText = buffer.lineText(lineIndex);
            renderLine(lineText, row, size.getColumns());
        }
    }

    private void renderLine(String lineText, int row, int maxColumns) {
        int col = 0;
        int offset = 0;
        while (offset < lineText.length() && col < maxColumns) {
            int codePoint = lineText.codePointAt(offset);
            int charCount = Character.charCount(codePoint);
            String ch = lineText.substring(offset, offset + charCount);

            int displayWidth = getDisplayWidth(codePoint);
            if (col + displayWidth > maxColumns) {
                break;
            }

            screen.setCharacter(col, row, TextCharacter.fromString(ch)[0]);

            col += displayWidth;
            offset += charCount;
        }
    }

    private void positionCursor(Window window, TerminalSize size) {
        var buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        int displayStart = window.getDisplayStartLine();

        int screenRow = lineIndex - displayStart;
        if (screenRow < 0 || screenRow >= size.getRows()) {
            return;
        }

        int col = computeColumnForOffset(buffer.lineText(lineIndex), point - lineStart);
        screen.setCursorPosition(new TerminalPosition(col, screenRow));
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
    private static final Set<Character.UnicodeBlock> FULL_WIDTH_BLOCKS = Set.of(
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
}
