package io.github.shomah4a.alle.core.styling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RegexStylerRegionMatchTest {

    private static final Face CODE_FACE = Face.CODE;

    @Nested
    class 同一行内のリージョン {

        @Test
        void openとcloseが同一行にある場合その範囲にFaceが適用される() {
            var rules = Lists.immutable.of((StylingRule)
                    new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE));
            var styler = new RegexStyler(rules);

            var result = styler.styleLineWithState("aaa /* comment */ bbb", StylingState.NONE);

            assertEquals(1, result.spans().size());
            // "/* comment */" は cp 4〜17
            assertEquals(4, result.spans().get(0).start());
            assertEquals(17, result.spans().get(0).end());
            assertEquals(CODE_FACE, result.spans().get(0).face());
            assertFalse(result.nextState().isInRegion());
        }

        @Test
        void 同一行内に複数のリージョンがある場合すべて適用される() {
            var rules = Lists.immutable.of((StylingRule)
                    new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE));
            var styler = new RegexStyler(rules);

            // "/* a */ b /* c */" は17文字
            // /* a */ = cp 0〜7, /* c */ = cp 10〜17 (末尾のスペース含む)
            var result = styler.styleLineWithState("/* a */ b /* c */", StylingState.NONE);

            assertEquals(2, result.spans().size());
            assertEquals(0, result.spans().get(0).start());
            assertEquals(7, result.spans().get(0).end());
            assertEquals(10, result.spans().get(1).start());
            assertEquals(17, result.spans().get(1).end());
            assertFalse(result.nextState().isInRegion());
        }
    }

    @Nested
    class 複数行にまたがるリージョン {

        @Test
        void openがあってcloseがない行ではリージョン継続状態になる() {
            var rules = Lists.immutable.of((StylingRule)
                    new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE));
            var styler = new RegexStyler(rules);

            var result = styler.styleLineWithState("aaa /* comment", StylingState.NONE);

            assertEquals(1, result.spans().size());
            assertEquals(4, result.spans().get(0).start());
            assertEquals(14, result.spans().get(0).end());
            assertTrue(result.nextState().isInRegion());
        }

        @Test
        void リージョン継続中の行全体にFaceが適用される() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule);
            var styler = new RegexStyler(rules);
            var inRegion = new StylingState(java.util.Optional.of(regionRule));

            var result = styler.styleLineWithState("middle of comment", inRegion);

            assertEquals(1, result.spans().size());
            assertEquals(0, result.spans().get(0).start());
            assertEquals(17, result.spans().get(0).end());
            assertEquals(CODE_FACE, result.spans().get(0).face());
            assertTrue(result.nextState().isInRegion());
        }

        @Test
        void リージョン継続中にcloseが見つかればリージョン終了する() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule);
            var styler = new RegexStyler(rules);
            var inRegion = new StylingState(java.util.Optional.of(regionRule));

            var result = styler.styleLineWithState("end */ normal", inRegion);

            assertEquals(1, result.spans().size());
            assertEquals(0, result.spans().get(0).start());
            assertEquals(6, result.spans().get(0).end());
            assertEquals(CODE_FACE, result.spans().get(0).face());
            assertFalse(result.nextState().isInRegion());
        }

        @Test
        void 三行にまたがるリージョンが正しく処理される() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule);
            var styler = new RegexStyler(rules);

            // 1行目: open
            var r1 = styler.styleLineWithState("/* start", StylingState.NONE);
            assertTrue(r1.nextState().isInRegion());
            assertEquals(1, r1.spans().size());
            assertEquals(0, r1.spans().get(0).start());
            assertEquals(8, r1.spans().get(0).end());

            // 2行目: 中間
            var r2 = styler.styleLineWithState("middle", r1.nextState());
            assertTrue(r2.nextState().isInRegion());
            assertEquals(1, r2.spans().size());
            assertEquals(0, r2.spans().get(0).start());
            assertEquals(6, r2.spans().get(0).end());

            // 3行目: close
            var r3 = styler.styleLineWithState("end */", r2.nextState());
            assertFalse(r3.nextState().isInRegion());
            assertEquals(1, r3.spans().size());
            assertEquals(0, r3.spans().get(0).start());
            assertEquals(6, r3.spans().get(0).end());
        }
    }

    @Nested
    class リージョン内の他ルール {

        @Test
        void リージョン継続中は他のルールが無視される() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule, (StylingRule)
                    new StylingRule.PatternMatch(Pattern.compile("keyword"), Face.KEYWORD));
            var styler = new RegexStyler(rules);
            var inRegion = new StylingState(java.util.Optional.of(regionRule));

            var result = styler.styleLineWithState("keyword in comment", inRegion);

            assertEquals(1, result.spans().size());
            assertEquals(CODE_FACE, result.spans().get(0).face());
            assertEquals(0, result.spans().get(0).start());
            assertEquals(18, result.spans().get(0).end());
        }
    }

    @Nested
    class ルール優先順位 {

        @Test
        void 先に定義されたPatternMatchがRegionMatchのopenより優先される() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("/\\*.*?\\*/"), Face.KEYWORD),
                    (StylingRule)
                            new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE));
            var styler = new RegexStyler(rules);

            var result = styler.styleLineWithState("/* comment */", StylingState.NONE);

            assertEquals(1, result.spans().size());
            assertEquals(Face.KEYWORD, result.spans().get(0).face());
        }
    }

    @Nested
    class 空行と特殊ケース {

        @Test
        void 空行でリージョン継続中の場合は状態が維持される() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule);
            var styler = new RegexStyler(rules);
            var inRegion = new StylingState(java.util.Optional.of(regionRule));

            var result = styler.styleLineWithState("", inRegion);

            assertTrue(result.spans().isEmpty());
            assertTrue(result.nextState().isInRegion());
        }

        @Test
        void 空行でリージョン外の場合はNONE状態が維持される() {
            var rules = Lists.immutable.of((StylingRule)
                    new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE));
            var styler = new RegexStyler(rules);

            var result = styler.styleLineWithState("", StylingState.NONE);

            assertTrue(result.spans().isEmpty());
            assertFalse(result.nextState().isInRegion());
        }
    }

    @Nested
    class openとcloseが同一パターンのリージョン {

        @Test
        void 同一パターンで開始と終了が別行で正しく動作する() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("^---$"), Pattern.compile("^---$"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule);
            var styler = new RegexStyler(rules);

            var r1 = styler.styleLineWithState("---", StylingState.NONE);
            assertTrue(r1.nextState().isInRegion());

            var r2 = styler.styleLineWithState("content", r1.nextState());
            assertTrue(r2.nextState().isInRegion());

            var r3 = styler.styleLineWithState("---", r2.nextState());
            assertFalse(r3.nextState().isInRegion());
        }

        @Test
        void 同一パターンで開始終了を繰り返しても状態が正しく遷移する() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("^---$"), Pattern.compile("^---$"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule);
            var styler = new RegexStyler(rules);

            var r1 = styler.styleLineWithState("---", StylingState.NONE);
            assertTrue(r1.nextState().isInRegion());

            var r2 = styler.styleLineWithState("---", r1.nextState());
            assertFalse(r2.nextState().isInRegion());

            var r3 = styler.styleLineWithState("---", r2.nextState());
            assertTrue(r3.nextState().isInRegion());

            var r4 = styler.styleLineWithState("---", r3.nextState());
            assertFalse(r4.nextState().isInRegion());
        }
    }

    @Nested
    class リージョン終了後の通常ルール処理 {

        @Test
        void リージョン終了後の同一行で他のルールが適用される() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule, (StylingRule)
                    new StylingRule.PatternMatch(Pattern.compile("keyword"), Face.KEYWORD));
            var styler = new RegexStyler(rules);
            var inRegion = new StylingState(java.util.Optional.of(regionRule));

            var result = styler.styleLineWithState("end */ keyword", inRegion);

            assertEquals(2, result.spans().size());
            assertEquals(CODE_FACE, result.spans().get(0).face());
            assertEquals(0, result.spans().get(0).start());
            assertEquals(6, result.spans().get(0).end());
            assertEquals(Face.KEYWORD, result.spans().get(1).face());
            assertFalse(result.nextState().isInRegion());
        }

        @Test
        void リージョン終了後の同一行で新しいリージョンが開始される() {
            var regionRule = new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE);
            var rules = Lists.immutable.of((StylingRule) regionRule);
            var styler = new RegexStyler(rules);
            var inRegion = new StylingState(java.util.Optional.of(regionRule));

            var result = styler.styleLineWithState("end */ text /* new", inRegion);

            assertEquals(2, result.spans().size());
            assertEquals(0, result.spans().get(0).start());
            assertEquals(6, result.spans().get(0).end());
            assertTrue(result.nextState().isInRegion());
        }
    }

    @Nested
    class RegionMatchのないルールセットでの後方互換性 {

        @Test
        void RegionMatchルールがない場合styleLineWithStateはstyleLineと同じ結果を返す() {
            var rules = Lists.immutable.of(
                    (StylingRule) new StylingRule.PatternMatch(Pattern.compile("`[^`]+`"), Face.CODE));
            var styler = new RegexStyler(rules);

            var styleResult = styler.styleLine("Hello `code` World");
            var styleLineResult = styler.styleLineWithState("Hello `code` World", StylingState.NONE);

            assertEquals(styleResult.size(), styleLineResult.spans().size());
            for (int i = 0; i < styleResult.size(); i++) {
                assertEquals(styleResult.get(i), styleLineResult.spans().get(i));
            }
            assertFalse(styleLineResult.nextState().isInRegion());
        }

        @Test
        void RegionMatchルールがない場合は常にNONE状態が返される() {
            var rules =
                    Lists.immutable.of((StylingRule) new StylingRule.LineMatch(Pattern.compile("^#.*"), Face.HEADING));
            var styler = new RegexStyler(rules);

            var r1 = styler.styleLineWithState("# heading", StylingState.NONE);
            assertFalse(r1.nextState().isInRegion());

            var r2 = styler.styleLineWithState("normal", r1.nextState());
            assertFalse(r2.nextState().isInRegion());
        }
    }

    @Nested
    class 絵文字を含むリージョン {

        @Test
        void 絵文字を含む行でリージョンのコードポイントオフセットが正しい() {
            var rules = Lists.immutable.of((StylingRule)
                    new StylingRule.RegionMatch(Pattern.compile("/\\*"), Pattern.compile("\\*/"), CODE_FACE));
            var styler = new RegexStyler(rules);

            // 😀はサロゲートペア（char 2個、コードポイント1個）
            var result = styler.styleLineWithState("😀 /* comment */", StylingState.NONE);

            assertEquals(1, result.spans().size());
            // コードポイント: 😀(0) ' '(1) '/'(2) '*'(3) ... '*'(13) '/'(14)
            assertEquals(2, result.spans().get(0).start());
            assertEquals(15, result.spans().get(0).end());
            assertFalse(result.nextState().isInRegion());
        }
    }
}
