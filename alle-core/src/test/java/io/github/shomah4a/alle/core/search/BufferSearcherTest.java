package io.github.shomah4a.alle.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferSearcherTest {

    @Nested
    class 前方検索 {

        @Test
        void 検索位置以降で最初のマッチを返す() {
            var result = BufferSearcher.searchForward("hello world hello", "hello", 0, true);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void 検索位置を指定して途中からマッチを探す() {
            var result = BufferSearcher.searchForward("hello world hello", "hello", 1, true);
            assertTrue(result.isPresent());
            assertEquals(12, result.get().start());
            assertEquals(17, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void マッチがない場合はemptyを返す() {
            var result = BufferSearcher.searchForward("hello world", "xyz", 0, true);
            assertTrue(result.isEmpty());
        }

        @Test
        void 空クエリではemptyを返す() {
            var result = BufferSearcher.searchForward("hello", "", 0, true);
            assertTrue(result.isEmpty());
        }

        @Test
        void 末尾を超えた場合にラップアラウンドで先頭から検索する() {
            var result = BufferSearcher.searchForward("hello world hello", "hello", 13, true);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertTrue(result.get().wrapped());
        }

        @Test
        void ラップアラウンドしてもマッチがない場合はemptyを返す() {
            var result = BufferSearcher.searchForward("hello world", "xyz", 5, true);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class 後方検索 {

        @Test
        void 検索位置より前で最後のマッチを返す() {
            var result = BufferSearcher.searchBackward("hello world hello", "hello", 17, true);
            assertTrue(result.isPresent());
            assertEquals(12, result.get().start());
            assertEquals(17, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void 検索位置を指定して途中からマッチを探す() {
            var result = BufferSearcher.searchBackward("hello world hello", "hello", 12, true);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void マッチがない場合はemptyを返す() {
            var result = BufferSearcher.searchBackward("hello world", "xyz", 11, true);
            assertTrue(result.isEmpty());
        }

        @Test
        void 空クエリではemptyを返す() {
            var result = BufferSearcher.searchBackward("hello", "", 5, true);
            assertTrue(result.isEmpty());
        }

        @Test
        void 先頭を超えた場合にラップアラウンドで末尾から検索する() {
            var result = BufferSearcher.searchBackward("hello world hello", "hello", 0, true);
            assertTrue(result.isPresent());
            assertEquals(12, result.get().start());
            assertEquals(17, result.get().end());
            assertTrue(result.get().wrapped());
        }

        @Test
        void 検索開始位置自身は範囲に含まない() {
            // "hello hello" で fromCodePointOffset=6 (二つ目の hello の開始位置)。
            // 直前の hello (0) を返し、二つ目自身は返さないこと。
            var result = BufferSearcher.searchBackward("hello hello", "hello", 6, true);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertFalse(result.get().wrapped());
        }
    }

    @Nested
    class サロゲートペア対応 {

        @Test
        void サロゲートペアを含むテキストで正しいコードポイントオフセットを返す() {
            // 🎉 は U+1F389 (サロゲートペア、char 2個)
            var text = "a🎉bc";
            var result = BufferSearcher.searchForward(text, "bc", 0, true);
            assertTrue(result.isPresent());
            // コードポイント単位: a=0, 🎉=1, b=2, c=3
            assertEquals(2, result.get().start());
            assertEquals(4, result.get().end());
        }

        @Test
        void サロゲートペア直後のfromCodePointOffsetでsearchBackwardが直前のマッチを返す() {
            // text = "hello🎉hello" (コードポイント単位: h=0..o=4, 🎉=5, h=6..o=10)
            // fromCodePointOffset=6 (絵文字直後、char では 7) で後方検索すると
            // 内部で fromCharOffset-1=6 (下位サロゲート位置) になるが、
            // lastIndexOf 内のサロゲートスキップで先頭の "hello" (char=0, codepoint=0) を返すこと
            var text = "hello🎉hello";
            var result = BufferSearcher.searchBackward(text, "hello", 6, true);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void サロゲートペアを含むクエリで検索できる() {
            var text = "hello🎉world";
            var result = BufferSearcher.searchForward(text, "🎉", 0, true);
            assertTrue(result.isPresent());
            assertEquals(5, result.get().start());
            assertEquals(6, result.get().end());
        }
    }

    @Nested
    class ケース無視前方検索 {

        @Test
        void caseSensitiveがfalseなら大文字小文字を区別しない() {
            var result = BufferSearcher.searchForward("Hello World", "hello", 0, false);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
        }

        @Test
        void caseSensitiveがtrueなら異ケースはマッチしない() {
            var result = BufferSearcher.searchForward("Hello World", "hello", 0, true);
            assertTrue(result.isEmpty());
        }

        @Test
        void ケース無視でラップアラウンドする() {
            var result = BufferSearcher.searchForward("Hello World hello", "hello", 13, false);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertTrue(result.get().wrapped());
        }

        @Test
        void ケース無視でもサロゲートペアのコードポイントオフセットが正しい() {
            var text = "🎉Hello World";
            var result = BufferSearcher.searchForward(text, "hello", 0, false);
            assertTrue(result.isPresent());
            // コードポイント単位: 🎉=0, H=1, e=2, l=3, l=4, o=5
            assertEquals(1, result.get().start());
            assertEquals(6, result.get().end());
        }
    }

    @Nested
    class ケース無視後方検索 {

        @Test
        void caseSensitiveがfalseなら大文字小文字を区別しない() {
            var result = BufferSearcher.searchBackward("Hello World hello", "HELLO", 17, false);
            assertTrue(result.isPresent());
            assertEquals(12, result.get().start());
            assertEquals(17, result.get().end());
        }

        @Test
        void caseSensitiveがtrueなら異ケースはマッチしない() {
            var result = BufferSearcher.searchBackward("Hello World", "hello", 11, true);
            assertTrue(result.isEmpty());
        }

        @Test
        void ケース無視で先頭からのラップアラウンドが起きる() {
            var result = BufferSearcher.searchBackward("Hello World HELLO", "hello", 0, false);
            assertTrue(result.isPresent());
            assertEquals(12, result.get().start());
            assertEquals(17, result.get().end());
            assertTrue(result.get().wrapped());
        }

        @Test
        void ケース無視で検索開始位置自身を範囲に含まない() {
            // "Hello hello" で fromCodePointOffset=6 (二つ目の hello)。
            // 一つ目の Hello (0) を返し、二つ目自身は返さないこと。
            var result = BufferSearcher.searchBackward("Hello hello", "hello", 6, false);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertFalse(result.get().wrapped());
        }
    }
}
