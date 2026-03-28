package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RegexStylerTest {

    @Nested
    class LineMatchルール {

        @Test
        void パターンに一致する行全体にFaceが適用される() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.LineMatch(Pattern.compile("^#\\s.*"), FaceName.HEADING));
            var styler = new RegexStyler(rules);

            var spans = styler.styleLine("# Hello");

            assertEquals(1, spans.size());
            assertEquals(0, spans.get(0).start());
            assertEquals(7, spans.get(0).end());
            assertEquals(FaceName.HEADING, spans.get(0).faceName());
        }

        @Test
        void パターンに一致しない行ではスパンが生成されない() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.LineMatch(Pattern.compile("^#\\s.*"), FaceName.HEADING));
            var styler = new RegexStyler(rules);

            var spans = styler.styleLine("Hello World");

            assertTrue(spans.isEmpty());
        }
    }

    @Nested
    class PatternMatchルール {

        @Test
        void マッチした部分にFaceが適用される() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("`[^`]+`"), FaceName.CODE));
            var styler = new RegexStyler(rules);

            var spans = styler.styleLine("Hello `code` World");

            assertEquals(1, spans.size());
            assertEquals(6, spans.get(0).start());
            assertEquals(12, spans.get(0).end());
            assertEquals(FaceName.CODE, spans.get(0).faceName());
        }

        @Test
        void 複数マッチがある場合すべてスパンが生成される() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("`[^`]+`"), FaceName.CODE));
            var styler = new RegexStyler(rules);

            var spans = styler.styleLine("`a` and `b`");

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
                    (StylingRule) new StylingRule.LineMatch(Pattern.compile("^#\\s.*"), FaceName.HEADING),
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("`[^`]+`"), FaceName.CODE));
            var styler = new RegexStyler(rules);

            // 見出し行内のコードスパンは見出しFaceで上書きされる
            var spans = styler.styleLine("# Hello `code`");

            assertEquals(1, spans.size());
            assertEquals(FaceName.HEADING, spans.get(0).faceName());
        }
    }

    @Nested
    class 空行と特殊ケース {

        @Test
        void 空行ではスパンが生成されない() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.LineMatch(Pattern.compile("^#\\s.*"), FaceName.HEADING));
            var styler = new RegexStyler(rules);

            var spans = styler.styleLine("");

            assertTrue(spans.isEmpty());
        }

        @Test
        void ルールが空の場合スパンが生成されない() {
            var styler = new RegexStyler(Lists.immutable.empty());

            var spans = styler.styleLine("Hello World");

            assertTrue(spans.isEmpty());
        }
    }

    @Nested
    class コードポイント変換 {

        @Test
        void 絵文字を含む行で正しいコードポイントオフセットを返す() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("`[^`]+`"), FaceName.CODE));
            var styler = new RegexStyler(rules);

            // 😀はサロゲートペア（char 2個、コードポイント1個）
            var spans = styler.styleLine("😀 `code` end");

            assertEquals(1, spans.size());
            // コードポイント: 😀(idx=0) ' '(idx=1) `(idx=2) c(3) o(4) d(5) e(6) `(7)
            assertEquals(2, spans.get(0).start());
            assertEquals(8, spans.get(0).end());
        }

        @Test
        void charOffsetToCodePointOffsetが正しく変換する() {
            // "😀abc" → 😀はchar 2個
            assertEquals(0, RegexStyler.charOffsetToCodePointOffset("😀abc", 0));
            assertEquals(1, RegexStyler.charOffsetToCodePointOffset("😀abc", 2)); // 😀の後
            assertEquals(2, RegexStyler.charOffsetToCodePointOffset("😀abc", 3)); // aの後
        }
    }
}
