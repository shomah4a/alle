package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompletionResultTest {

    @Nested
    class resolve {

        @Test
        void 候補が0件なら入力をそのまま返す() {
            var result = CompletionResult.resolve("foo", Lists.immutable.empty());
            assertEquals("foo", result);
        }

        @Test
        void 候補が1件なら候補そのものを返す() {
            var result = CompletionResult.resolve("fo", Lists.immutable.of("foobar"));
            assertEquals("foobar", result);
        }

        @Test
        void 候補が複数なら最長共通プレフィックスを返す() {
            var result = CompletionResult.resolve("fo", Lists.immutable.of("foobar", "foobaz"));
            assertEquals("fooba", result);
        }

        @Test
        void 共通プレフィックスがない場合は空文字列を返す() {
            var result = CompletionResult.resolve("", Lists.immutable.of("abc", "xyz"));
            assertEquals("", result);
        }

        @Test
        void 候補が完全一致する場合はそのまま返す() {
            var result = CompletionResult.resolve("foo", Lists.immutable.of("foo", "foo"));
            assertEquals("foo", result);
        }

        @Test
        void 候補の長さが異なる場合も共通プレフィックスを返す() {
            var result = CompletionResult.resolve("t", Lists.immutable.of("test", "testing", "tests"));
            assertEquals("test", result);
        }
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
            var result = CompletionResult.resolveDetailed("fo", Lists.immutable.of("foobar"));
            var unique = assertInstanceOf(CompletionOutcome.Unique.class, result);
            assertEquals("foobar", unique.value());
        }

        @Test
        void 候補が複数ならPartialを返す() {
            var candidates = Lists.immutable.of("foobar", "foobaz");
            var result = CompletionResult.resolveDetailed("fo", candidates);
            var partial = assertInstanceOf(CompletionOutcome.Partial.class, result);
            assertEquals("fooba", partial.commonPrefix());
            assertEquals(candidates, partial.candidates());
        }

        @Test
        void 候補が複数で共通プレフィックスが入力と同じ場合もPartialを返す() {
            var candidates = Lists.immutable.of("abc", "axyz");
            var result = CompletionResult.resolveDetailed("a", candidates);
            var partial = assertInstanceOf(CompletionOutcome.Partial.class, result);
            assertEquals("a", partial.commonPrefix());
            assertEquals(candidates, partial.candidates());
        }
    }
}
