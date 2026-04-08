package io.github.shomah4a.alle.core;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;

/**
 * 行折り返し時の視覚行（visual line）計算ユーティリティ。
 * 1つのバッファ行を表示幅で分割した際の視覚行数や折り返し位置を計算する。
 */
public final class VisualLineUtil {

    private VisualLineUtil() {}

    /**
     * 折り返し位置（コードポイントオフセット）のリストを返す。
     * 各要素は視覚行の開始コードポイントオフセット。
     * 先頭行（オフセット0）は含まない。
     *
     * <p>全角文字が表示幅の末尾に収まらない場合、次の視覚行に送る。
     * タブは視覚行ローカルのカラム位置を基準として次のタブストップまで展開される。
     *
     * @param lineText バッファ行のテキスト
     * @param columns 表示幅（カラム数）
     * @param tabWidth タブストップ間隔
     * @return 折り返し位置のリスト（先頭行を除く各視覚行の開始コードポイントオフセット）
     */
    public static ImmutableList<Integer> computeVisualLineBreaks(String lineText, int columns, int tabWidth) {
        if (columns <= 0 || lineText.isEmpty()) {
            return Lists.immutable.empty();
        }
        MutableList<Integer> breaks = Lists.mutable.empty();
        int col = 0;
        int offset = 0;
        int cpIndex = 0;
        while (offset < lineText.length()) {
            int codePoint = lineText.codePointAt(offset);
            int displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint, col, tabWidth);
            if (col + displayWidth > columns) {
                breaks.add(cpIndex);
                col = 0;
                // 折り返し後の視覚行でタブ幅を再計算
                displayWidth = DisplayWidthUtil.getDisplayWidth(codePoint, col, tabWidth);
            }
            col += displayWidth;
            offset += Character.charCount(codePoint);
            cpIndex++;
        }
        return breaks.toImmutable();
    }

    /**
     * 1バッファ行の視覚行数を返す。
     * 空行は1行として扱う。
     *
     * @param lineText バッファ行のテキスト
     * @param columns 表示幅（カラム数）
     * @param tabWidth タブストップ間隔
     * @return 視覚行数（1以上）
     */
    public static int computeVisualLineCount(String lineText, int columns, int tabWidth) {
        if (columns <= 0 || lineText.isEmpty()) {
            return 1;
        }
        return computeVisualLineBreaks(lineText, columns, tabWidth).size() + 1;
    }

    /**
     * 指定コードポイントオフセットが属する視覚行番号（0始まり）を返す。
     *
     * @param lineText バッファ行のテキスト
     * @param columns 表示幅（カラム数）
     * @param cpOffset コードポイントオフセット（行ローカル）
     * @param tabWidth タブストップ間隔
     * @return 視覚行番号（0始まり）
     */
    public static int computeVisualLineForOffset(String lineText, int columns, int cpOffset, int tabWidth) {
        if (columns <= 0 || lineText.isEmpty()) {
            return 0;
        }
        var breaks = computeVisualLineBreaks(lineText, columns, tabWidth);
        int visualLine = 0;
        for (int i = 0; i < breaks.size(); i++) {
            if (cpOffset >= breaks.get(i)) {
                visualLine = i + 1;
            } else {
                break;
            }
        }
        return visualLine;
    }

    /**
     * 指定視覚行番号の開始コードポイントオフセットを返す。
     *
     * @param lineText バッファ行のテキスト
     * @param columns 表示幅（カラム数）
     * @param visualLine 視覚行番号（0始まり）
     * @param tabWidth タブストップ間隔
     * @return 開始コードポイントオフセット
     */
    public static int visualLineStartOffset(String lineText, int columns, int visualLine, int tabWidth) {
        if (visualLine <= 0 || columns <= 0 || lineText.isEmpty()) {
            return 0;
        }
        var breaks = computeVisualLineBreaks(lineText, columns, tabWidth);
        if (visualLine <= breaks.size()) {
            return breaks.get(visualLine - 1);
        }
        return (int) lineText.codePoints().count();
    }

    /**
     * 指定視覚行番号の終了コードポイントオフセット（排他）を返す。
     *
     * @param lineText バッファ行のテキスト
     * @param columns 表示幅（カラム数）
     * @param visualLine 視覚行番号（0始まり）
     * @param tabWidth タブストップ間隔
     * @return 終了コードポイントオフセット
     */
    public static int visualLineEndOffset(String lineText, int columns, int visualLine, int tabWidth) {
        if (columns <= 0 || lineText.isEmpty()) {
            return (int) lineText.codePoints().count();
        }
        var breaks = computeVisualLineBreaks(lineText, columns, tabWidth);
        if (visualLine < breaks.size()) {
            return breaks.get(visualLine);
        }
        return (int) lineText.codePoints().count();
    }
}
