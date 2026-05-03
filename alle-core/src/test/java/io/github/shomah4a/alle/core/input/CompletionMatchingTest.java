package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompletionMatchingTest {

    @Nested
    class startsWithIgnoreCase {

        @Test
        void 同じケースで前方一致する場合はtrue() {
            assertTrue(CompletionMatching.startsWithIgnoreCase("foobar", "foo"));
        }

        @Test
        void 異なるケースで前方一致する場合もtrue() {
            assertTrue(CompletionMatching.startsWithIgnoreCase("FooBar", "foo"));
            assertTrue(CompletionMatching.startsWithIgnoreCase("foobar", "FOO"));
            assertTrue(CompletionMatching.startsWithIgnoreCase("FooBar", "fOo"));
        }

        @Test
        void 前方一致しない場合はfalse() {
            assertFalse(CompletionMatching.startsWithIgnoreCase("foobar", "bar"));
        }

        @Test
        void プレフィックスが空文字なら常にtrue() {
            assertTrue(CompletionMatching.startsWithIgnoreCase("anything", ""));
            assertTrue(CompletionMatching.startsWithIgnoreCase("", ""));
        }

        @Test
        void 対象文字列がプレフィックスより短い場合はfalse() {
            assertFalse(CompletionMatching.startsWithIgnoreCase("fo", "foo"));
        }

        @Test
        void 非ASCII文字は同一ケースなら一致する() {
            assertTrue(CompletionMatching.startsWithIgnoreCase("日本語ファイル", "日本"));
        }
    }

    @Nested
    class startsWith {

        @Test
        void ignoreCaseがfalseならケース敏感に比較する() {
            assertTrue(CompletionMatching.startsWith("foobar", "foo", false));
            assertFalse(CompletionMatching.startsWith("FooBar", "foo", false));
        }

        @Test
        void ignoreCaseがtrueならケース無視で比較する() {
            assertTrue(CompletionMatching.startsWith("FooBar", "foo", true));
            assertTrue(CompletionMatching.startsWith("foobar", "FOO", true));
        }
    }
}
