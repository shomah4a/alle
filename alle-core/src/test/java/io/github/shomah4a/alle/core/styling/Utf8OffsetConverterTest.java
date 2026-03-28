package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class Utf8OffsetConverterTest {

    @Nested
    class ASCII文字列 {

        @Test
        void オフセット0はコードポイント0に対応する() {
            assertEquals(0, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("hello", 0));
        }

        @Test
        void ASCII文字はバイトオフセットとコードポイントオフセットが一致する() {
            assertEquals(3, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("hello", 3));
            assertEquals(5, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("hello", 5));
        }
    }

    @Nested
    class マルチバイト文字列 {

        @Test
        void 日本語文字はUTF8で3バイトのためバイトオフセットが3倍になる() {
            // "あいう" → UTF-8: 各3バイト
            // byte 0 → cp 0 ("あ"の先頭)
            // byte 3 → cp 1 ("い"の先頭)
            // byte 6 → cp 2 ("う"の先頭)
            assertEquals(0, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("あいう", 0));
            assertEquals(1, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("あいう", 3));
            assertEquals(2, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("あいう", 6));
            assertEquals(3, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("あいう", 9));
        }

        @Test
        void 絵文字はUTF8で4バイトのコードポイント() {
            // "😀abc" → UTF-8: 😀=4bytes, a=1, b=1, c=1
            // byte 0 → cp 0 (😀の先頭)
            // byte 4 → cp 1 (aの先頭)
            // byte 5 → cp 2 (bの先頭)
            assertEquals(0, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("😀abc", 0));
            assertEquals(1, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("😀abc", 4));
            assertEquals(2, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("😀abc", 5));
            assertEquals(3, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("😀abc", 6));
        }

        @Test
        void ASCII文字と日本語文字が混在する場合() {
            // "aあb" → UTF-8: a=1, あ=3, b=1
            // byte 0 → cp 0 (a)
            // byte 1 → cp 1 (あの先頭)
            // byte 4 → cp 2 (b)
            assertEquals(0, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("aあb", 0));
            assertEquals(1, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("aあb", 1));
            assertEquals(2, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("aあb", 4));
        }
    }

    @Nested
    class 空文字列 {

        @Test
        void 空文字列のオフセット0はコードポイント0() {
            assertEquals(0, Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset("", 0));
        }
    }

    @Nested
    class UTF8バイト長 {

        @Test
        void ASCII文字は1バイト() {
            assertEquals(1, Utf8OffsetConverter.utf8CodePointByteLength('a'));
            assertEquals(1, Utf8OffsetConverter.utf8CodePointByteLength('z'));
        }

        @Test
        void ラテン拡張は2バイト() {
            assertEquals(2, Utf8OffsetConverter.utf8CodePointByteLength('é'));
            assertEquals(2, Utf8OffsetConverter.utf8CodePointByteLength('ñ'));
        }

        @Test
        void 日本語は3バイト() {
            assertEquals(3, Utf8OffsetConverter.utf8CodePointByteLength('あ'));
            assertEquals(3, Utf8OffsetConverter.utf8CodePointByteLength('漢'));
        }

        @Test
        void 絵文字は4バイト() {
            assertEquals(4, Utf8OffsetConverter.utf8CodePointByteLength(0x1F600)); // 😀
        }
    }
}
