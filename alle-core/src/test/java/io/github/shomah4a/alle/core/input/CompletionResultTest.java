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
    }
}
