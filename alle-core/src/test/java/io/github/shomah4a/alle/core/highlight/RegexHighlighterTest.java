package io.github.shomah4a.alle.core.highlight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RegexHighlighterTest {

    @Nested
    class LineMatchルール {

        @Test
        void パターンに一致する行全体にFaceが適用される() {
            var rules = Lists.immutable.of(
                    (HighlightRule) new HighlightRule.LineMatch(Pattern.compile("^#\\s.*"), Face.HEADING));
            var highlighter = new RegexHighlighter(rules);

            var spans = highlighter.highlight("# Hello");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(7, spans.get(0).end());
            assertEquals(Face.HEADING, spans.get(0).face());
        }

        @Test
        void パターンに一致しない行ではスパンが生成されない() {
            var rules = Lists.immutable.of(
                    (HighlightRule) new HighlightRule.LineMatch(Pattern.compile("^#\\s.*"), Face.HEADING));
            var highlighter = new RegexHighlighter(rules);

            var spans = highlighter.highlight("Hello World");

            assertTrue(spans.isEmpty());
        }
    }

    @Nested
    class PatternMatchルール {

        @Test
        void マッチした部分にFaceが適用される() {
            var rules = Lists.immutable.of(
                    (HighlightRule) new HighlightRule.PatternMatch(Pattern.compile("`[^`]+`"), Face.CODE));
            var highlighter = new RegexHighlighter(rules);

            var spans = highlighter.highlight("Hello `code` World");

            assertEquals(1, spans.size());
            assertEquals(6, spans.get(0).start());
            assertEquals(12, spans.get(0).end());
            assertEquals(Face.CODE, spans.get(0).face());
        }

        @Test
        void 複数マッチがある場合すべてスパンが生成される() {
            var rules = Lists.immutable.of(
                    (HighlightRule) new HighlightRule.PatternMatch(Pattern.compile("`[^`]+`"), Face.CODE));
            var highlighter = new RegexHighlighter(rules);

            var spans = highlighter.highlight("`a` and `b`");

            assertEquals(2, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(3, spans.get(0).end());
            assertEquals(8, spans.get(1).start());
            assertEquals(11, spans.get(1).end());
        }
    }

    @Nested
    class ルールの優先順位 {

        @Test
        void 先に定義されたルールが優先される() {
            var rules = Lists.immutable.of(
                    (HighlightRule) new HighlightRule.LineMatch(Pattern.compile("^#\\s.*"), Face.HEADING),
                    (HighlightRule) new HighlightRule.PatternMatch(Pattern.compile("`[^`]+`"), Face.CODE));
            var highlighter = new RegexHighlighter(rules);

            // 見出し行内のコードスパンは見出しFaceで上書きされる
            var spans = highlighter.highlight("# Hello `code`");

            assertEquals(1, spans.size());
            assertEquals(Face.HEADING, spans.get(0).face());
        }
    }

    @Nested
    class 空行と特殊ケース {

        @Test
        void 空行ではスパンが生成されない() {
            var rules = Lists.immutable.of(
                    (HighlightRule) new HighlightRule.LineMatch(Pattern.compile("^#\\s.*"), Face.HEADING));
            var highlighter = new RegexHighlighter(rules);

            var spans = highlighter.highlight("");

            assertTrue(spans.isEmpty());
        }

        @Test
        void ルールが空の場合スパンが生成されない() {
            var highlighter = new RegexHighlighter(Lists.immutable.empty());

            var spans = highlighter.highlight("Hello World");

            assertTrue(spans.isEmpty());
        }
    }

    @Nested
    class コードポイント変換 {

        @Test
        void 絵文字を含む行で正しいコードポイントオフセットを返す() {
            var rules = Lists.immutable.of(
                    (HighlightRule) new HighlightRule.PatternMatch(Pattern.compile("`[^`]+`"), Face.CODE));
            var highlighter = new RegexHighlighter(rules);

            // 😀はサロゲートペア（char 2個、コードポイント1個）
            var spans = highlighter.highlight("😀 `code` end");

            assertEquals(1, spans.size());
            // コードポイント: 😀(idx=0) ' '(idx=1) `(idx=2) c(3) o(4) d(5) e(6) `(7)
            assertEquals(2, spans.get(0).start());
            assertEquals(8, spans.get(0).end());
        }

        @Test
        void charOffsetToCodePointOffsetが正しく変換する() {
            // "😀abc" → 😀はchar 2個
            assertEquals(0, RegexHighlighter.charOffsetToCodePointOffset("😀abc", 0));
            assertEquals(1, RegexHighlighter.charOffsetToCodePointOffset("😀abc", 2)); // 😀の後
            assertEquals(2, RegexHighlighter.charOffsetToCodePointOffset("😀abc", 3)); // aの後
        }
    }
}
