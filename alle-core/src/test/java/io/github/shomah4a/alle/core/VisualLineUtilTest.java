package io.github.shomah4a.alle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VisualLineUtilTest {

    @Nested
    class ComputeVisualLineBreaks {

        @Test
        void 表示幅に収まる行は折り返しなし() {
            var breaks = VisualLineUtil.computeVisualLineBreaks("hello", 10);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void 表示幅ちょうどの行は折り返しなし() {
            var breaks = VisualLineUtil.computeVisualLineBreaks("hello", 5);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void 表示幅を超える行は折り返される() {
            // "abcdefgh" を幅3で: abc|def|gh
            var breaks = VisualLineUtil.computeVisualLineBreaks("abcdefgh", 3);
            assertEquals(Lists.immutable.of(3, 6), breaks);
        }

        @Test
        void 全角文字が末尾に収まらない場合は次の視覚行に送る() {
            // "abあ" を幅3で: ab(2カラム) + あ(2カラム) = 4カラム
            // 幅3に "ab" は入る(2カラム)、"あ" は 2+2=4 > 3 なので次行
            var breaks = VisualLineUtil.computeVisualLineBreaks("abあ", 3);
            assertEquals(Lists.immutable.of(2), breaks);
        }

        @Test
        void 全角文字のみの行の折り返し() {
            // "あいう" 各2カラム = 計6カラム、幅4で: あい(4)|う(2)
            var breaks = VisualLineUtil.computeVisualLineBreaks("あいう", 4);
            assertEquals(Lists.immutable.of(2), breaks);
        }

        @Test
        void 全角文字で幅が奇数の場合() {
            // "あいう" 各2カラム = 計6カラム、幅3で:
            // あ(2カラム) → OK, い(2+2=4>3) → 次行
            // い(2カラム) → OK, う(2+2=4>3) → 次行
            var breaks = VisualLineUtil.computeVisualLineBreaks("あいう", 3);
            assertEquals(Lists.immutable.of(1, 2), breaks);
        }

        @Test
        void 空文字列は折り返しなし() {
            var breaks = VisualLineUtil.computeVisualLineBreaks("", 10);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void 表示幅0は折り返しなし() {
            var breaks = VisualLineUtil.computeVisualLineBreaks("hello", 0);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void 表示幅1でASCII文字() {
            // "abc" を幅1で: a|b|c
            var breaks = VisualLineUtil.computeVisualLineBreaks("abc", 1);
            assertEquals(Lists.immutable.of(1, 2), breaks);
        }

        @Test
        void 混在文字列の折り返し() {
            // "aあbいc" → a(1) あ(2) b(1) い(2) c(1) = 7カラム
            // 幅4で: a+あ=3, b=4 → OK, い=4+2=6 > 4 → 折り返し
            // 視覚行1: "aあb"(4カラム), 視覚行2: "いc"(3カラム)
            var breaks = VisualLineUtil.computeVisualLineBreaks("aあbいc", 4);
            assertEquals(Lists.immutable.of(3), breaks);
        }
    }

    @Nested
    class ComputeVisualLineCount {

        @Test
        void 表示幅に収まる行は1行() {
            assertEquals(1, VisualLineUtil.computeVisualLineCount("hello", 10));
        }

        @Test
        void 折り返しが発生すると行数が増える() {
            // "abcdefgh" を幅3で: abc|def|gh → 3行
            assertEquals(3, VisualLineUtil.computeVisualLineCount("abcdefgh", 3));
        }

        @Test
        void 空文字列は1行() {
            assertEquals(1, VisualLineUtil.computeVisualLineCount("", 10));
        }

        @Test
        void 表示幅0は1行() {
            assertEquals(1, VisualLineUtil.computeVisualLineCount("hello", 0));
        }
    }

    @Nested
    class ComputeVisualLineForOffset {

        @Test
        void 折り返しなしの行ではオフセットに関わらず0() {
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("hello", 10, 0));
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("hello", 10, 3));
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("hello", 10, 5));
        }

        @Test
        void 折り返しありの行で各視覚行のオフセットが正しい() {
            // "abcdefgh" を幅3で: abc|def|gh
            // cp0-2 → 視覚行0, cp3-5 → 視覚行1, cp6-7 → 視覚行2
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 0));
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 2));
            assertEquals(1, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 3));
            assertEquals(1, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 5));
            assertEquals(2, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 6));
        }

        @Test
        void 空文字列は0() {
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("", 10, 0));
        }
    }

    @Nested
    class VisualLineStartOffset {

        @Test
        void 視覚行0の開始オフセットは0() {
            assertEquals(0, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 0));
        }

        @Test
        void 折り返しの各視覚行の開始オフセット() {
            // "abcdefgh" を幅3で: abc|def|gh → breaks at cp3, cp6
            assertEquals(0, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 0));
            assertEquals(3, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 1));
            assertEquals(6, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 2));
        }

        @Test
        void 範囲外の視覚行番号は末尾を返す() {
            assertEquals(8, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 10));
        }
    }

    @Nested
    class VisualLineEndOffset {

        @Test
        void 折り返しなしの行の終了オフセットは文字数() {
            assertEquals(5, VisualLineUtil.visualLineEndOffset("hello", 10, 0));
        }

        @Test
        void 折り返しの各視覚行の終了オフセット() {
            // "abcdefgh" を幅3で: abc|def|gh → breaks at cp3, cp6
            assertEquals(3, VisualLineUtil.visualLineEndOffset("abcdefgh", 3, 0));
            assertEquals(6, VisualLineUtil.visualLineEndOffset("abcdefgh", 3, 1));
            assertEquals(8, VisualLineUtil.visualLineEndOffset("abcdefgh", 3, 2));
        }

        @Test
        void 最終視覚行の終了オフセットは文字数() {
            assertEquals(8, VisualLineUtil.visualLineEndOffset("abcdefgh", 3, 2));
        }
    }
}
