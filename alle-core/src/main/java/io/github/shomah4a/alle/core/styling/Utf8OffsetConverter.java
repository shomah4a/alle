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
