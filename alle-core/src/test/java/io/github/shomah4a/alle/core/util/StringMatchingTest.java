package io.github.shomah4a.alle.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StringMatchingTest {

    @Nested
    class startsWithIgnoreCase {

        @Test
        void 同じケースで前方一致する場合はtrue() {
            assertTrue(StringMatching.startsWithIgnoreCase("foobar", "foo"));
        }

        @Test
        void 異なるケースで前方一致する場合もtrue() {
            assertTrue(StringMatching.startsWithIgnoreCase("FooBar", "foo"));
            assertTrue(StringMatching.startsWithIgnoreCase("foobar", "FOO"));
            assertTrue(StringMatching.startsWithIgnoreCase("FooBar", "fOo"));
        }

        @Test
        void 前方一致しない場合はfalse() {
            assertFalse(StringMatching.startsWithIgnoreCase("foobar", "bar"));
        }

        @Test
        void プレフィックスが空文字なら常にtrue() {
            assertTrue(StringMatching.startsWithIgnoreCase("anything", ""));
            assertTrue(StringMatching.startsWithIgnoreCase("", ""));
        }

        @Test
        void 対象文字列がプレフィックスより短い場合はfalse() {
            assertFalse(StringMatching.startsWithIgnoreCase("fo", "foo"));
        }

        @Test
        void 非ASCII文字は同一ケースなら一致する() {
            assertTrue(StringMatching.startsWithIgnoreCase("日本語ファイル", "日本"));
        }
    }

    @Nested
    class startsWith {

        @Test
        void ignoreCaseがfalseならケース敏感に比較する() {
            assertTrue(StringMatching.startsWith("foobar", "foo", false));
            assertFalse(StringMatching.startsWith("FooBar", "foo", false));
        }

        @Test
        void ignoreCaseがtrueならケース無視で比較する() {
            assertTrue(StringMatching.startsWith("FooBar", "foo", true));
            assertTrue(StringMatching.startsWith("foobar", "FOO", true));
        }
    }

    @Nested
    class containsUpperCase {

        @Test
        void 空文字列はfalse() {
            assertFalse(StringMatching.containsUpperCase(""));
        }

        @Test
        void 全て小文字ならfalse() {
            assertFalse(StringMatching.containsUpperCase("hello world"));
        }

        @Test
        void 大文字を1つでも含めばtrue() {
            assertTrue(StringMatching.containsUpperCase("Hello"));
            assertTrue(StringMatching.containsUpperCase("hellO"));
            assertTrue(StringMatching.containsUpperCase("HELLO"));
        }

        @Test
        void 数字や記号や空白はfalse() {
            assertFalse(StringMatching.containsUpperCase("123-456 _!@#"));
        }

        @Test
        void 日本語文字はケース概念がないためfalse() {
            assertFalse(StringMatching.containsUpperCase("日本語ファイル"));
        }

        @Test
        void 絵文字などサロゲートペアはfalse() {
            assertFalse(StringMatching.containsUpperCase("hello🎉world"));
        }

        @Test
        void ギリシャ大文字やキリル大文字もtrue() {
            assertTrue(StringMatching.containsUpperCase("Α")); // U+0391 GREEK CAPITAL LETTER ALPHA
            assertTrue(StringMatching.containsUpperCase("Б")); // U+0411 CYRILLIC CAPITAL LETTER BE
        }

        @Test
        void タイトルケース文字もtrue() {
            // U+01C5 LATIN CAPITAL LETTER D WITH SMALL LETTER Z WITH CARON (Title Case)
            assertTrue(StringMatching.containsUpperCase("ǅ"));
        }
    }

    @Nested
    class indexOf {

        @Test
        void ignoreCaseがfalseのとき大文字小文字を区別する() {
            assertEquals(0, StringMatching.indexOf("Hello", "Hello", 0, false));
            assertEquals(-1, StringMatching.indexOf("Hello", "hello", 0, false));
        }

        @Test
        void ignoreCaseがtrueのとき大文字小文字を区別しない() {
            assertEquals(0, StringMatching.indexOf("Hello", "hello", 0, true));
            assertEquals(0, StringMatching.indexOf("hello", "HELLO", 0, true));
            assertEquals(0, StringMatching.indexOf("HeLLo", "hElLo", 0, true));
        }

        @Test
        void fromIndexの位置以降から検索する() {
            assertEquals(6, StringMatching.indexOf("hello hello", "hello", 1, false));
            assertEquals(6, StringMatching.indexOf("hello Hello", "hello", 1, true));
        }

        @Test
        void マッチがなければマイナス1を返す() {
            assertEquals(-1, StringMatching.indexOf("hello", "world", 0, false));
            assertEquals(-1, StringMatching.indexOf("hello", "world", 0, true));
        }

        @Test
        void 空クエリならマイナス1を返す() {
            assertEquals(-1, StringMatching.indexOf("hello", "", 0, false));
            assertEquals(-1, StringMatching.indexOf("hello", "", 0, true));
        }

        @Test
        void 負のfromIndexは0として扱う() {
            assertEquals(0, StringMatching.indexOf("hello", "hello", -3, true));
        }

        @Test
        void textがqueryより短い場合はマイナス1を返す() {
            assertEquals(-1, StringMatching.indexOf("hi", "hello", 0, true));
        }

        @Test
        void 下位サロゲート位置からマッチを開始しない() {
            // 🎉 (U+1F389) はサロゲートペア (D83C DF89)。char index 1 は下位サロゲート位置。
            // クエリが下位サロゲート + 何かになる組み立てはあり得ないが、
            // 仮に query が text の char 1 から始まる位置で regionMatches する場合でもスキップする保証
            // を確認する。"🎉a" の char 配列は [D83C, DF89, a] で、低サロゲート位置 (1) からマッチ開始しない
            String text = "🎉a";
            // "abc" などはマッチしないので、検索結果は文字列前方から正しくマッチする位置を返すこと
            assertEquals(2, StringMatching.indexOf(text, "a", 0, true));
        }

        @Test
        void BMP内ユニコード文字のケース折りに対応する() {
            // Greek alpha: U+0391 (capital) ↔ U+03B1 (small)
            assertEquals(0, StringMatching.indexOf("Α", "α", 0, true));
            assertEquals(0, StringMatching.indexOf("α", "Α", 0, true));
        }
    }

    @Nested
    class lastIndexOf {

        @Test
        void ignoreCaseがfalseのとき大文字小文字を区別する() {
            assertEquals(0, StringMatching.lastIndexOf("Hello", "Hello", 5, false));
            assertEquals(-1, StringMatching.lastIndexOf("Hello", "hello", 5, false));
        }

        @Test
        void ignoreCaseがtrueのとき大文字小文字を区別しない() {
            assertEquals(6, StringMatching.lastIndexOf("Hello hello", "HELLO", 11, true));
            assertEquals(6, StringMatching.lastIndexOf("Hello hello", "Hello", 11, true));
        }

        @Test
        void fromIndex以下の最後のマッチを返す() {
            // "hello hello" で fromIndex=10 → start <= 10 の最大マッチ位置 = 6
            assertEquals(6, StringMatching.lastIndexOf("hello hello", "hello", 10, false));
            // fromIndex=5 → start <= 5 の最大マッチ位置 = 0
            assertEquals(0, StringMatching.lastIndexOf("hello hello", "hello", 5, false));
        }

        @Test
        void マッチがなければマイナス1を返す() {
            assertEquals(-1, StringMatching.lastIndexOf("hello", "world", 5, false));
            assertEquals(-1, StringMatching.lastIndexOf("hello", "world", 5, true));
        }

        @Test
        void 空クエリならマイナス1を返す() {
            assertEquals(-1, StringMatching.lastIndexOf("hello", "", 5, false));
            assertEquals(-1, StringMatching.lastIndexOf("hello", "", 5, true));
        }

        @Test
        void 負のfromIndexはマイナス1を返す() {
            assertEquals(-1, StringMatching.lastIndexOf("hello", "hello", -1, true));
            assertEquals(-1, StringMatching.lastIndexOf("hello", "hello", -1, false));
        }

        @Test
        void textがqueryより短い場合はマイナス1を返す() {
            assertEquals(-1, StringMatching.lastIndexOf("hi", "hello", 2, true));
        }

        @Test
        void 下位サロゲート位置からマッチを開始しない() {
            String text = "a🎉";
            // 末尾から後方に走査するとき、char index 2 (低サロゲート) はスキップする
            assertEquals(0, StringMatching.lastIndexOf(text, "a", 2, true));
        }

        @Test
        void BMP内ユニコード文字のケース折りに対応する() {
            assertEquals(0, StringMatching.lastIndexOf("Α", "α", 0, true));
        }
    }
}
