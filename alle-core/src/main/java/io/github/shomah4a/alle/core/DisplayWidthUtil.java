package io.github.shomah4a.alle.core;

import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Unicode文字の表示幅を計算するユーティリティ。
 * CJK文字等の全角文字は2カラム、タブ文字はタブストップ境界まで展開、それ以外は1カラムとして扱う。
 */
public final class DisplayWidthUtil {

    private DisplayWidthUtil() {}

    private static final ImmutableSet<Character.UnicodeBlock> FULL_WIDTH_BLOCKS = Sets.immutable.with(
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
            Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
            Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
            Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS,
            Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT,
            Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
            Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS,
            Character.UnicodeBlock.HIRAGANA,
            Character.UnicodeBlock.KATAKANA,
            Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
            Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
            Character.UnicodeBlock.HANGUL_JAMO,
            Character.UnicodeBlock.HANGUL_SYLLABLES,
            Character.UnicodeBlock.EMOTICONS,
            Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS);

    /**
     * コードポイントの表示幅を返す。
     * タブ文字は現在カラム位置から次のタブストップまでの距離を返し、
     * CJK文字等の全角文字は2、それ以外は1。
     *
     * @param codePoint 対象コードポイント
     * @param currentColumn この文字が配置されるカラム位置（視覚行先頭を0とする）
     * @param tabWidth タブストップ間隔（1以上）
     * @return 表示カラム幅
     */
    public static int getDisplayWidth(int codePoint, int currentColumn, int tabWidth) {
        if (codePoint == '\t') {
            return tabWidth - (currentColumn % tabWidth);
        }
        if (isFullWidth(codePoint)) {
            return 2;
        }
        return 1;
    }

    /**
     * 全角文字かどうかを判定する。
     * East Asian Width が Wide (W) または Fullwidth (F) の場合にtrueを返す。
     */
    public static boolean isFullWidth(int codePoint) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        if (block == null) {
            return false;
        }
        if (FULL_WIDTH_BLOCKS.contains(block)) {
            return true;
        }
        // HALFWIDTH_AND_FULLWIDTH_FORMSは半角文字も含むため、全角範囲のみ判定
        // Lanterna の TerminalTextUtils.isCharCJK と一致させる (< 0xFF61)
        if (block.equals(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS)) {
            return codePoint < 0xFF61;
        }
        return false;
    }

    /**
     * 行内のコードポイントオフセットから画面上のカラム位置を計算する。
     *
     * @param lineText 行テキスト
     * @param codePointOffset 対象コードポイントオフセット
     * @param tabWidth タブストップ間隔
     * @return 視覚行先頭を0としたカラム位置
     */
    public static int computeColumnForOffset(String lineText, int codePointOffset, int tabWidth) {
        int col = 0;
        int offset = 0;
        int cpCount = 0;
        while (offset < lineText.length() && cpCount < codePointOffset) {
            int codePoint = lineText.codePointAt(offset);
            col += getDisplayWidth(codePoint, col, tabWidth);
            offset += Character.charCount(codePoint);
            cpCount++;
        }
        return col;
    }

    /**
     * 行テキスト内の [startCp, endCp) 範囲の表示幅を計算する。
     * startCp のカラムを 0 として累積する視覚行ローカル基準。
     * 折り返しモードで視覚行内のカラム差分を求める用途に使う。
     */
    public static int computeColumnWidthInRange(String lineText, int startCp, int endCp, int tabWidth) {
        if (endCp <= startCp) {
            return 0;
        }
        int offset = 0;
        int cpIndex = 0;
        while (offset < lineText.length() && cpIndex < startCp) {
            int codePoint = lineText.codePointAt(offset);
            offset += Character.charCount(codePoint);
            cpIndex++;
        }
        int col = 0;
        while (offset < lineText.length() && cpIndex < endCp) {
            int codePoint = lineText.codePointAt(offset);
            col += getDisplayWidth(codePoint, col, tabWidth);
            offset += Character.charCount(codePoint);
            cpIndex++;
        }
        return col;
    }

    /**
     * 行テキストの [startCp, endCp) 範囲内で、指定表示カラムに最も近いコードポイントオフセットを返す。
     * カラムは startCp を 0 とする視覚行ローカル基準で計算する。
     * 指定カラムがタブや全角文字の途中に位置する場合、その文字の手前で止める。
     *
     * @param lineText 行テキスト
     * @param startCp 計算開始コードポイントオフセット
     * @param endCp 計算終了コードポイントオフセット（排他的）
     * @param targetColumn startCp を 0 とした目標カラム位置
     * @param tabWidth タブストップ間隔
     * @return targetColumn に到達または超えない最大のコードポイントオフセット
     */
    public static int computeOffsetForColumn(
            String lineText, int startCp, int endCp, int targetColumn, int tabWidth) {
        int offset = 0;
        int cpIndex = 0;

        while (offset < lineText.length() && cpIndex < startCp) {
            int codePoint = lineText.codePointAt(offset);
            offset += Character.charCount(codePoint);
            cpIndex++;
        }

        int resultCp = startCp;
        int currentCol = 0;
        while (offset < lineText.length() && cpIndex < endCp) {
            int codePoint = lineText.codePointAt(offset);
            int displayWidth = getDisplayWidth(codePoint, currentCol, tabWidth);
            if (currentCol + displayWidth > targetColumn) {
                break;
            }
            currentCol += displayWidth;
            offset += Character.charCount(codePoint);
            cpIndex++;
            resultCp = cpIndex;
        }
        return Math.min(resultCp, endCp);
    }

    /**
     * 指定カラムが全角文字やタブの途中に位置する場合、その文字の先頭カラムに丸める。
     * 文字境界上であればそのまま返す。
     *
     * @return 文字先頭カラムに丸めた値
     */
    public static int snapColumnToCharBoundary(String lineText, int column, int tabWidth) {
        if (column <= 0) {
            return 0;
        }
        int col = 0;
        int offset = 0;
        while (offset < lineText.length()) {
            int codePoint = lineText.codePointAt(offset);
            int width = getDisplayWidth(codePoint, col, tabWidth);
            if (col + width > column) {
                return col;
            }
            col += width;
            if (col >= column) {
                return column;
            }
            offset += Character.charCount(codePoint);
        }
        return col;
    }
}
