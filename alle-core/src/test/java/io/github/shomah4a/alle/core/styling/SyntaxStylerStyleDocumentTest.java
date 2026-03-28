package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SyntaxStylerStyleDocumentTest {

    @Nested
    class 単一行ドキュメント {

        @Test
        void PatternMatchルールが適用される() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("\\bdef\\b"), FaceName.KEYWORD));
            var styler = new RegexStyler(rules);

            var result = styler.styleDocument(Lists.immutable.of("def foo():"));

            assertEquals(1, result.size());
            assertEquals(1, result.get(0).size());
            assertEquals(new StyledSpan(0, 3, FaceName.KEYWORD), result.get(0).get(0));
        }

        @Test
        void 空行のドキュメントではスパンが生成されない() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("\\bdef\\b"), FaceName.KEYWORD));
            var styler = new RegexStyler(rules);

            var result = styler.styleDocument(Lists.immutable.of(""));

            assertEquals(1, result.size());
            assertTrue(result.get(0).isEmpty());
        }
    }

    @Nested
    class 複数行ドキュメント {

        @Test
        void 各行が独立してスタイリングされる() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("\\bdef\\b"), FaceName.KEYWORD),
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("\"[^\"]*\""), FaceName.STRING));
            var styler = new RegexStyler(rules);

            var result = styler.styleDocument(Lists.immutable.of("def foo():", "    return \"hello\""));

            assertEquals(2, result.size());
            // 1行目: def
            assertEquals(1, result.get(0).size());
            assertEquals(new StyledSpan(0, 3, FaceName.KEYWORD), result.get(0).get(0));
            // 2行目: "hello"
            assertEquals(1, result.get(1).size());
            assertEquals(new StyledSpan(11, 18, FaceName.STRING), result.get(1).get(0));
        }
    }

    @Nested
    class RegionMatchによる複数行スタイリング {

        @Test
        void リージョンが複数行にまたがる場合に状態が引き継がれる() {
            var rules = Lists.immutable.of((StylingRule)
                    new StylingRule.RegionMatch(Pattern.compile("```"), Pattern.compile("```"), FaceName.CODE));
            var styler = new RegexStyler(rules);

            var result = styler.styleDocument(Lists.immutable.of("before", "```", "code line", "```", "after"));

            assertEquals(5, result.size());
            // "before": スパンなし
            assertTrue(result.get(0).isEmpty());
            // "```": CODE
            assertEquals(1, result.get(1).size());
            assertEquals(FaceName.CODE, result.get(1).get(0).faceName());
            // "code line": 全体にCODE
            assertEquals(1, result.get(2).size());
            assertEquals(FaceName.CODE, result.get(2).get(0).faceName());
            assertEquals(0, result.get(2).get(0).start());
            assertEquals(9, result.get(2).get(0).end());
            // "```": CODE（close部分）
            assertEquals(1, result.get(3).size());
            assertEquals(FaceName.CODE, result.get(3).get(0).faceName());
            // "after": スパンなし
            assertTrue(result.get(4).isEmpty());
        }

        @Test
        void styleLineWithStateと同一の結果を返す() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("#.*$"), FaceName.COMMENT),
                    (StylingRule) new StylingRule.RegionMatch(
                            Pattern.compile("\"\"\""), Pattern.compile("\"\"\""), FaceName.STRING));
            var styler = new RegexStyler(rules);

            var lines = Lists.immutable.of("# comment", "x = \"\"\"", "multi", "line", "\"\"\"", "y = 1");

            // styleDocument の結果
            var docResult = styler.styleDocument(lines);

            // styleLineWithState を手動で呼んだ結果
            StylingState state = styler.initialState();
            for (int i = 0; i < lines.size(); i++) {
                var lineResult = styler.styleLineWithState(lines.get(i), state);
                ListIterable<StyledSpan> docSpans = docResult.get(i);
                ListIterable<StyledSpan> lineSpans = lineResult.spans();
                assertEquals(lineSpans.size(), docSpans.size(), "行 " + i + " のスパン数が一致しない");
                for (int j = 0; j < lineSpans.size(); j++) {
                    assertEquals(lineSpans.get(j), docSpans.get(j), "行 " + i + " のスパン " + j + " が一致しない");
                }
                state = lineResult.nextState();
            }
        }
    }

    @Nested
    class 空のドキュメント {

        @Test
        void 行が0件の場合は空リストを返す() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("\\bdef\\b"), FaceName.KEYWORD));
            var styler = new RegexStyler(rules);

            var result = styler.styleDocument(Lists.immutable.empty());

            assertTrue(result.isEmpty());
        }
    }
}
