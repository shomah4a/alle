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
            var result = BufferSearcher.searchForward("hello world hello", "hello", 0);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void 検索位置を指定して途中からマッチを探す() {
            var result = BufferSearcher.searchForward("hello world hello", "hello", 1);
            assertTrue(result.isPresent());
            assertEquals(12, result.get().start());
            assertEquals(17, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void マッチがない場合はemptyを返す() {
            var result = BufferSearcher.searchForward("hello world", "xyz", 0);
            assertTrue(result.isEmpty());
        }

        @Test
        void 空クエリではemptyを返す() {
            var result = BufferSearcher.searchForward("hello", "", 0);
            assertTrue(result.isEmpty());
        }

        @Test
        void 末尾を超えた場合にラップアラウンドで先頭から検索する() {
            var result = BufferSearcher.searchForward("hello world hello", "hello", 13);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertTrue(result.get().wrapped());
        }

        @Test
        void ラップアラウンドしてもマッチがない場合はemptyを返す() {
            var result = BufferSearcher.searchForward("hello world", "xyz", 5);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class 後方検索 {

        @Test
        void 検索位置より前で最後のマッチを返す() {
            var result = BufferSearcher.searchBackward("hello world hello", "hello", 17);
            assertTrue(result.isPresent());
            assertEquals(12, result.get().start());
            assertEquals(17, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void 検索位置を指定して途中からマッチを探す() {
            var result = BufferSearcher.searchBackward("hello world hello", "hello", 12);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(5, result.get().end());
            assertFalse(result.get().wrapped());
        }

        @Test
        void マッチがない場合はemptyを返す() {
            var result = BufferSearcher.searchBackward("hello world", "xyz", 11);
            assertTrue(result.isEmpty());
        }

        @Test
        void 空クエリではemptyを返す() {
            var result = BufferSearcher.searchBackward("hello", "", 5);
            assertTrue(result.isEmpty());
        }

        @Test
        void 先頭を超えた場合にラップアラウンドで末尾から検索する() {
            var result = BufferSearcher.searchBackward("hello world hello", "hello", 0);
            assertTrue(result.isPresent());
            assertEquals(12, result.get().start());
            assertEquals(17, result.get().end());
            assertTrue(result.get().wrapped());
        }
    }

    @Nested
    class サロゲートペア対応 {

        @Test
        void サロゲートペアを含むテキストで正しいコードポイントオフセットを返す() {
            // 🎉 は U+1F389 (サロゲートペア、char 2個)
            var text = "a🎉bc";
            var result = BufferSearcher.searchForward(text, "bc", 0);
            assertTrue(result.isPresent());
            // コードポイント単位: a=0, 🎉=1, b=2, c=3
            assertEquals(2, result.get().start());
            assertEquals(4, result.get().end());
        }

        @Test
        void サロゲートペアを含むクエリで検索できる() {
            var text = "hello🎉world";
            var result = BufferSearcher.searchForward(text, "🎉", 0);
            assertTrue(result.isPresent());
            assertEquals(5, result.get().start());
            assertEquals(6, result.get().end());
        }
    }
}
