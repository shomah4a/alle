package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompletionResultTest {

    private static CompletionCandidate t(String value) {
        return CompletionCandidate.terminal(value);
    }

    @Nested
    class resolveDetailed {

        @Test
        void 候補が0件ならNoMatchを返す() {
            var result = CompletionResult.resolveDetailed("foo", Lists.immutable.empty());
            assertInstanceOf(CompletionOutcome.NoMatch.class, result);
        }

        @Test
        void 候補が1件ならUniqueを返す() {
            var result = CompletionResult.resolveDetailed("fo", Lists.immutable.of(t("foobar")));
            var unique = assertInstanceOf(CompletionOutcome.Unique.class, result);
            assertEquals("foobar", unique.candidate().value());
        }

        @Test
        void 候補が複数ならPartialを返す() {
            var candidates = Lists.immutable.of(t("foobar"), t("foobaz"));
            var result = CompletionResult.resolveDetailed("fo", candidates);
            var partial = assertInstanceOf(CompletionOutcome.Partial.class, result);
            assertEquals("fooba", partial.commonPrefix());
            assertEquals(candidates, partial.candidates());
        }

        @Test
        void 候補が複数で共通プレフィックスが入力と同じ場合もPartialを返す() {
            var candidates = Lists.immutable.of(t("abc"), t("axyz"));
            var result = CompletionResult.resolveDetailed("a", candidates);
            var partial = assertInstanceOf(CompletionOutcome.Partial.class, result);
            assertEquals("a", partial.commonPrefix());
            assertEquals(candidates, partial.candidates());
        }

        @Test
        void ignoreCase版でケース無視で共通プレフィックスを算出する() {
            var candidates = Lists.immutable.of(t("Source/foo"), t("source/bar"));
            var result = CompletionResult.resolveDetailed("src", candidates, true);
            var partial = assertInstanceOf(CompletionOutcome.Partial.class, result);
            // 先頭候補のケース表記で返る
            assertEquals("Source/", partial.commonPrefix());
        }

        @Test
        void ignoreCaseがfalseなら従来通りケース敏感で算出する() {
            var candidates = Lists.immutable.of(t("Source/foo"), t("source/bar"));
            var result = CompletionResult.resolveDetailed("src", candidates, false);
            var partial = assertInstanceOf(CompletionOutcome.Partial.class, result);
            assertEquals("", partial.commonPrefix());
        }
    }

    @Nested
    class longestCommonPrefix {

        @Test
        void ケース敏感版がケース違いを区別する() {
            var result = CompletionResult.longestCommonPrefix(Lists.immutable.of("Source/foo", "source/bar"));
            assertEquals("", result);
        }

        @Test
        void ignoreCase版が先頭候補のケース表記で返す() {
            var result = CompletionResult.longestCommonPrefix(Lists.immutable.of("Source/foo", "source/bar"), true);
            assertEquals("Source/", result);
        }

        @Test
        void ignoreCase版で順序を入れ替えると先頭候補のケース表記で返る() {
            var result = CompletionResult.longestCommonPrefix(Lists.immutable.of("source/bar", "Source/foo"), true);
            assertEquals("source/", result);
        }

        @Test
        void ignoreCase版でケースのみ異なる完全一致は先頭候補のままを返す() {
            var result = CompletionResult.longestCommonPrefix(Lists.immutable.of("foo", "FOO"), true);
            assertEquals("foo", result);
        }

        @Test
        void サロゲートペア共通部分は壊れずに返る() {
            var s1 = "😀abc"; // U+1F600 😀
            var s2 = "😀xyz";
            var result = CompletionResult.longestCommonPrefix(Lists.immutable.of(s1, s2));
            assertEquals("😀", result);
        }

        @Test
        void サロゲートペアの直前で分岐するケースで境界が壊れない() {
            var s1 = "x😀abc"; // U+1F600
            var s2 = "x😁xyz"; // U+1F601
            var result = CompletionResult.longestCommonPrefix(Lists.immutable.of(s1, s2));
            assertEquals("x", result);
        }

        @Test
        void 候補1件なら全文がそのまま返る() {
            var result = CompletionResult.longestCommonPrefix(Lists.immutable.of("abcdef"));
            assertEquals("abcdef", result);
        }
    }
}
