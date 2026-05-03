package io.github.shomah4a.alle.core.util;

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
}
