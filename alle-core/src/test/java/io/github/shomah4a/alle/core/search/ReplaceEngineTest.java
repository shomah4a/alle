package io.github.shomah4a.alle.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReplaceEngineTest {

    @Nested
    class リテラル検索 {

        @Test
        void 空クエリでは検索しない() {
            var result = ReplaceEngine.findLiteralNext("hello", "", 0, 5);
            assertTrue(result.isEmpty());
        }

        @Test
        void 先頭からのマッチを返す() {
            var result = ReplaceEngine.findLiteralNext("foo bar foo", "foo", 0, 11);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(3, result.get().end());
        }

        @Test
        void 検索開始位置以降のマッチを返す() {
            var result = ReplaceEngine.findLiteralNext("foo bar foo", "foo", 3, 11);
            assertTrue(result.isPresent());
            assertEquals(8, result.get().start());
            assertEquals(11, result.get().end());
        }

        @Test
        void 範囲を越えるマッチは採用しない() {
            // "foo bar foo" で最後の foo は 8..11 にある。rangeEnd=10 だと採用不可
            var result = ReplaceEngine.findLiteralNext("foo bar foo", "foo", 3, 10);
            assertTrue(result.isEmpty());
        }

        @Test
        void マルチバイト文字のマッチ() {
            // "あいう" で "い" は codepoint 1..2
            var result = ReplaceEngine.findLiteralNext("あいう", "い", 0, 3);
            assertTrue(result.isPresent());
            assertEquals(1, result.get().start());
            assertEquals(2, result.get().end());
        }

        @Test
        void サロゲートペアを含むマッチ() {
            // U+1F600 (😀) は 1 コードポイント（char 2 個）
            String text = "a😀b😀c";
            var result = ReplaceEngine.findLiteralNext(text, "😀", 0, 5);
            assertTrue(result.isPresent());
            assertEquals(1, result.get().start());
            assertEquals(2, result.get().end());
        }

        @Test
        void searchFromがrangeEnd以上ならemptyを返す() {
            var result = ReplaceEngine.findLiteralNext("abc", "a", 3, 3);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class 正規表現検索 {

        @Test
        void 単純な文字列パターンがマッチする() {
            var pattern = Pattern.compile("foo");
            var result = ReplaceEngine.findRegexpNext("say foo bar", pattern, 0, 11);
            assertTrue(result.isPresent());
            assertEquals(4, result.get().start());
            assertEquals(7, result.get().end());
            assertEquals("foo", result.get().groups().get(0));
        }

        @Test
        void キャプチャグループを取得できる() {
            var pattern = Pattern.compile("(\\w+)=(\\w+)");
            var result = ReplaceEngine.findRegexpNext("a=b", pattern, 0, 3);
            assertTrue(result.isPresent());
            assertEquals("a=b", result.get().groups().get(0));
            assertEquals("a", result.get().groups().get(1));
            assertEquals("b", result.get().groups().get(2));
        }

        @Test
        void 範囲を超えるマッチは返さない() {
            // "foo bar foo" の 2 つめ foo は 8..11。rangeEnd=10 なら返さない
            var pattern = Pattern.compile("foo");
            var result = ReplaceEngine.findRegexpNext("foo bar foo", pattern, 3, 10);
            assertTrue(result.isEmpty());
        }

        @Test
        void 空マッチも検出できる() {
            // "\b" は 0 幅マッチ
            var pattern = Pattern.compile("\\b");
            var result = ReplaceEngine.findRegexpNext("foo", pattern, 0, 3);
            assertTrue(result.isPresent());
            assertEquals(0, result.get().start());
            assertEquals(0, result.get().end());
        }

        @Test
        void キャプチャが存在しないpatternでもgroups0にマッチ全体が入る() {
            var pattern = Pattern.compile("\\d+");
            var result = ReplaceEngine.findRegexpNext("abc123def", pattern, 0, 9);
            assertTrue(result.isPresent());
            assertEquals(1, result.get().groups().size());
            assertEquals("123", result.get().groups().get(0));
        }
    }

    @Nested
    class テンプレート展開 {

        private static ReplaceMatch.Regex buildMatch(String... groups) {
            return new ReplaceMatch.Regex(0, 0, Lists.immutable.of(groups));
        }

        @Test
        void 単純なリテラルはそのまま返す() {
            var match = buildMatch("whole");
            assertEquals("plain", ReplaceEngine.expandEmacsReplacement("plain", match));
        }

        @Test
        void マッチ全体を返す_アンパサンド記法() {
            var match = buildMatch("Hello");
            assertEquals("[Hello]", ReplaceEngine.expandEmacsReplacement("[\\&]", match));
        }

        @Test
        void マッチ全体を返す_ゼロ記法() {
            var match = buildMatch("Hello");
            assertEquals("[Hello]", ReplaceEngine.expandEmacsReplacement("[\\0]", match));
        }

        @Test
        void キャプチャグループを展開する() {
            var match = buildMatch("a=b", "a", "b");
            assertEquals("b=a", ReplaceEngine.expandEmacsReplacement("\\2=\\1", match));
        }

        @Test
        void バックスラッシュのエスケープ() {
            var match = buildMatch("x");
            assertEquals("a\\b", ReplaceEngine.expandEmacsReplacement("a\\\\b", match));
        }

        @Test
        void 存在しないキャプチャ参照は空文字列() {
            var match = buildMatch("x");
            // group 1 が存在しない状態
            assertEquals("<>", ReplaceEngine.expandEmacsReplacement("<\\1>", match));
        }

        @Test
        void 他のエスケープはリテラル文字として扱う() {
            var match = buildMatch("x");
            // \n は Emacs ではリテラル n （Java の改行にはならない）
            assertEquals("an", ReplaceEngine.expandEmacsReplacement("a\\n", match));
        }

        @Test
        void ドル記号は特殊扱いしない() {
            var match = buildMatch("x");
            assertEquals("$1", ReplaceEngine.expandEmacsReplacement("$1", match));
        }

        @Test
        void 末尾のバックスラッシュはそのまま保持される() {
            var match = buildMatch("x");
            assertEquals("abc\\", ReplaceEngine.expandEmacsReplacement("abc\\", match));
        }

        @Test
        void 空のテンプレートは空文字列() {
            var match = buildMatch("x");
            assertEquals("", ReplaceEngine.expandEmacsReplacement("", match));
        }
    }

    @Test
    void 範囲外と範囲内の両方を正しく扱う_リテラル() {
        // "aaa" で "a" を searchFrom=1, rangeEnd=2
        var result = ReplaceEngine.findLiteralNext("aaa", "a", 1, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.get().start());
        assertEquals(2, result.get().end());

        var result2 = ReplaceEngine.findLiteralNext("aaa", "a", 2, 2);
        assertFalse(result2.isPresent());
    }
}
