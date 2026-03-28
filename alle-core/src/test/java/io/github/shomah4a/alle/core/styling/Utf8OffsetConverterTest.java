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
    class コードポイントオフセットからUTF8バイトオフセットへの変換 {

        @Test
        void ASCII文字列ではオフセットが一致する() {
            assertEquals(0, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("hello", 0));
            assertEquals(3, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("hello", 3));
            assertEquals(5, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("hello", 5));
        }

        @Test
        void 日本語文字列ではコードポイント1つにつき3バイト() {
            assertEquals(0, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("あいう", 0));
            assertEquals(3, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("あいう", 1));
            assertEquals(6, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("あいう", 2));
            assertEquals(9, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("あいう", 3));
        }

        @Test
        void 絵文字を含む文字列で正しく変換される() {
            // "😀abc" → 😀=4bytes, a=1, b=1, c=1
            assertEquals(0, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("😀abc", 0));
            assertEquals(4, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("😀abc", 1));
            assertEquals(5, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("😀abc", 2));
        }

        @Test
        void 改行を含むテキストで正しく変換される() {
            // "ab\ncd" → a=1, b=1, \n=1, c=1, d=1
            assertEquals(0, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("ab\ncd", 0));
            assertEquals(2, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("ab\ncd", 2));
            assertEquals(3, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("ab\ncd", 3));
            assertEquals(5, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("ab\ncd", 5));
        }

        @Test
        void 改行と日本語が混在するテキストで正しく変換される() {
            // "あ\nい" → あ=3, \n=1, い=3
            assertEquals(0, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("あ\nい", 0));
            assertEquals(3, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("あ\nい", 1));
            assertEquals(4, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("あ\nい", 2));
            assertEquals(7, Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset("あ\nい", 3));
        }
    }

    @Nested
    class コードポイントオフセットから行と列への変換 {

        @Test
        void ASCII単一行のテキストでrow0とcolumnがバイトオフセットになる() {
            assertEquals(new Utf8Position(0, 3), Utf8OffsetConverter.codePointOffsetToRowColumn("hello", 3));
        }

        @Test
        void 改行を超えた位置ではrowが増加しcolumnがリセットされる() {
            // "ab\ncd" → offset 3='c' → 行1の先頭なので column=0
            assertEquals(new Utf8Position(1, 0), Utf8OffsetConverter.codePointOffsetToRowColumn("ab\ncd", 3));
            // offset 4='d' → 行1の2文字目なので column=1
            assertEquals(new Utf8Position(1, 1), Utf8OffsetConverter.codePointOffsetToRowColumn("ab\ncd", 4));
        }

        @Test
        void 改行位置ではcolumnが0になる() {
            // "ab\n" → offset 3 → 改行を処理した直後なのでrow=1, col=0
            assertEquals(new Utf8Position(1, 0), Utf8OffsetConverter.codePointOffsetToRowColumn("ab\n", 3));
        }

        @Test
        void 日本語を含む行では列がUTF8バイト単位になる() {
            // "あいう" → offset 2='う' → row=0, column=6 (あ=3bytes + い=3bytes)
            assertEquals(new Utf8Position(0, 6), Utf8OffsetConverter.codePointOffsetToRowColumn("あいう", 2));
        }

        @Test
        void 複数行で日本語を含む場合の列計算が正しい() {
            // "ab\nあい" → offset 4='い' → row=1, column=3 (あ=3bytes)
            assertEquals(new Utf8Position(1, 3), Utf8OffsetConverter.codePointOffsetToRowColumn("ab\nあい", 4));
        }

        @Test
        void テキスト先頭ではrow0_column0() {
            assertEquals(new Utf8Position(0, 0), Utf8OffsetConverter.codePointOffsetToRowColumn("hello", 0));
        }

        @Test
        void 連続する改行を正しく処理する() {
            // "a\n\nb" → offset 3='b' → row=2, 行先頭なので column=0
            assertEquals(new Utf8Position(2, 0), Utf8OffsetConverter.codePointOffsetToRowColumn("a\n\nb", 3));
        }

        @Test
        void 絵文字を含む行で列がUTF8バイト単位になる() {
            // "a😀b" → offset 2='b' → row=0, column=5 (a=1byte + 😀=4bytes)
            assertEquals(new Utf8Position(0, 5), Utf8OffsetConverter.codePointOffsetToRowColumn("a😀b", 2));
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
