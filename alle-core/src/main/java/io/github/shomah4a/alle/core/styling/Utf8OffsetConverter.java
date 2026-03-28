package io.github.shomah4a.alle.core.styling;

/**
 * UTF-8バイトオフセットとコードポイントオフセットの相互変換ユーティリティ。
 *
 * <p>Tree-sitterはUTF-8バイト単位の位置情報を返すが、
 * {@link StyledSpan}はコードポイント単位のオフセットを使用するため、変換が必要。
 */
public final class Utf8OffsetConverter {

    private Utf8OffsetConverter() {}

    /**
     * 行テキスト内のUTF-8バイトオフセットをコードポイントオフセットに変換する。
     *
     * @param lineText 行のテキスト
     * @param utf8ByteOffset UTF-8バイトオフセット
     * @return コードポイントオフセット
     */
    public static int utf8ByteOffsetToCodePointOffset(String lineText, int utf8ByteOffset) {
        int bytePos = 0;
        int codePointOffset = 0;
        int charIndex = 0;

        while (bytePos < utf8ByteOffset && charIndex < lineText.length()) {
            int codePoint = lineText.codePointAt(charIndex);
            int utf8Len = utf8CodePointByteLength(codePoint);
            bytePos += utf8Len;
            codePointOffset++;
            charIndex += Character.charCount(codePoint);
        }

        return codePointOffset;
    }

    /**
     * テキスト先頭からのコードポイントオフセットをUTF-8バイトオフセットに変換する。
     *
     * <p>改行文字を含むフルテキストに対して使用する。
     *
     * @param text テキスト全体
     * @param codePointOffset コードポイントオフセット
     * @return UTF-8バイトオフセット
     */
    public static int codePointOffsetToUtf8ByteOffset(String text, int codePointOffset) {
        int bytePos = 0;
        int cpCount = 0;
        int charIndex = 0;

        while (cpCount < codePointOffset && charIndex < text.length()) {
            int codePoint = text.codePointAt(charIndex);
            bytePos += utf8CodePointByteLength(codePoint);
            cpCount++;
            charIndex += Character.charCount(codePoint);
        }

        return bytePos;
    }

    /**
     * テキスト先頭からのコードポイントオフセットに対応する行番号と行内UTF-8バイト列オフセットを計算する。
     *
     * <p>Tree-sitterのTSPointに渡すための値を計算する。
     * rowは0始まりの行番号、columnはその行の先頭からのUTF-8バイトオフセット。
     *
     * @param text テキスト全体
     * @param codePointOffset コードポイントオフセット
     * @return 行番号と行内UTF-8バイトオフセット
     */
    public static Utf8Position codePointOffsetToRowColumn(String text, int codePointOffset) {
        int row = 0;
        int columnBytes = 0;
        int cpCount = 0;
        int charIndex = 0;

        while (cpCount < codePointOffset && charIndex < text.length()) {
            int codePoint = text.codePointAt(charIndex);
            if (codePoint == '\n') {
                row++;
                columnBytes = 0;
            } else {
                columnBytes += utf8CodePointByteLength(codePoint);
            }
            cpCount++;
            charIndex += Character.charCount(codePoint);
        }

        return new Utf8Position(row, columnBytes);
    }

    /**
     * コードポイントのUTF-8エンコード時のバイト数を返す。
     */
    static int utf8CodePointByteLength(int codePoint) {
        if (codePoint <= 0x7F) {
            return 1;
        } else if (codePoint <= 0x7FF) {
            return 2;
        } else if (codePoint <= 0xFFFF) {
            return 3;
        } else {
            return 4;
        }
    }
}
