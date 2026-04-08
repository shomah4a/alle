package io.github.shomah4a.alle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VisualLineUtilTest {

    private static final int TAB8 = 8;

    @Nested
    class ComputeVisualLineBreaks {

        @Test
        void 表示幅に収まる行は折り返しなし() {
            var breaks = VisualLineUtil.computeVisualLineBreaks("hello", 10, TAB8);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void 表示幅ちょうどの行は折り返しなし() {
            var breaks = VisualLineUtil.computeVisualLineBreaks("hello", 5, TAB8);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void 表示幅を超える行は折り返される() {
            // "abcdefgh" を幅3で: abc|def|gh
            var breaks = VisualLineUtil.computeVisualLineBreaks("abcdefgh", 3, TAB8);
            assertEquals(Lists.immutable.of(3, 6), breaks);
        }

        @Test
        void 全角文字が末尾に収まらない場合は次の視覚行に送る() {
            // "abあ" を幅3で: ab(2カラム) + あ(2カラム) = 4カラム
            // 幅3に "ab" は入る(2カラム)、"あ" は 2+2=4 > 3 なので次行
            var breaks = VisualLineUtil.computeVisualLineBreaks("abあ", 3, TAB8);
            assertEquals(Lists.immutable.of(2), breaks);
        }

        @Test
        void 全角文字のみの行の折り返し() {
            // "あいう" 各2カラム = 計6カラム、幅4で: あい(4)|う(2)
            var breaks = VisualLineUtil.computeVisualLineBreaks("あいう", 4, TAB8);
            assertEquals(Lists.immutable.of(2), breaks);
        }

        @Test
        void 全角文字で幅が奇数の場合() {
            // "あいう" 各2カラム = 計6カラム、幅3で:
            // あ(2カラム) → OK, い(2+2=4>3) → 次行
            // い(2カラム) → OK, う(2+2=4>3) → 次行
            var breaks = VisualLineUtil.computeVisualLineBreaks("あいう", 3, TAB8);
            assertEquals(Lists.immutable.of(1, 2), breaks);
        }

        @Test
        void 空文字列は折り返しなし() {
            var breaks = VisualLineUtil.computeVisualLineBreaks("", 10, TAB8);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void 表示幅0は折り返しなし() {
            var breaks = VisualLineUtil.computeVisualLineBreaks("hello", 0, TAB8);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void 表示幅1でASCII文字() {
            // "abc" を幅1で: a|b|c
            var breaks = VisualLineUtil.computeVisualLineBreaks("abc", 1, TAB8);
            assertEquals(Lists.immutable.of(1, 2), breaks);
        }

        @Test
        void 混在文字列の折り返し() {
            // "aあbいc" → a(1) あ(2) b(1) い(2) c(1) = 7カラム
            // 幅4で: a+あ=3, b=4 → OK, い=4+2=6 > 4 → 折り返し
            // 視覚行1: "aあb"(4カラム), 視覚行2: "いc"(3カラム)
            var breaks = VisualLineUtil.computeVisualLineBreaks("aあbいc", 4, TAB8);
            assertEquals(Lists.immutable.of(3), breaks);
        }

        @Test
        void 先頭タブは8カラムを消費する() {
            // "\tab" tab=8 → tab(8カラム), a(1), b(1) = 10カラム
            // 幅12 → 折り返しなし
            var breaks = VisualLineUtil.computeVisualLineBreaks("\tab", 12, TAB8);
            assertEquals(Lists.immutable.empty(), breaks);
        }

        @Test
        void タブ後の内容が幅に収まらない場合は折り返し() {
            // "\tabc" tab=8 → tab(8), a(1), b(1), c(1) = 11カラム
            // 幅10 → tab,a,b まで入る(10カラム)、c は 11 > 10 なので次行
            var breaks = VisualLineUtil.computeVisualLineBreaks("\tabc", 10, TAB8);
            assertEquals(Lists.immutable.of(3), breaks);
        }

        @Test
        void タブが表示幅を超える場合は次の視覚行に送られる() {
            // "ab\tc" tab8 幅6
            // cp0 'a': col=0→1
            // cp1 'b': col=1→2
            // cp2 '\t': col=2, 幅 = 8-2=6, 2+6=8 > 6 → 折り返し、col=0
            //          折り返し後の幅 = 8-0=8 で col+=8=8
            // cp3 'c': col=8, 幅1, 8+1>6 → 折り返し、col=0→1
            var breaks = VisualLineUtil.computeVisualLineBreaks("ab\tc", 6, TAB8);
            assertEquals(Lists.immutable.of(2, 3), breaks);
        }

        @Test
        void タブ幅4を指定した場合() {
            // "\tab" tab=4 → tab(4), a(1), b(1) = 6カラム
            // 幅5で: tab,a まで(5カラム)、b は 6 > 5 なので次行
            var breaks = VisualLineUtil.computeVisualLineBreaks("\tab", 5, 4);
            assertEquals(Lists.immutable.of(2), breaks);
        }
    }

    @Nested
    class ComputeVisualLineCount {

        @Test
        void 表示幅に収まる行は1行() {
            assertEquals(1, VisualLineUtil.computeVisualLineCount("hello", 10, TAB8));
        }

        @Test
        void 折り返しが発生すると行数が増える() {
            // "abcdefgh" を幅3で: abc|def|gh → 3行
            assertEquals(3, VisualLineUtil.computeVisualLineCount("abcdefgh", 3, TAB8));
        }

        @Test
        void 空文字列は1行() {
            assertEquals(1, VisualLineUtil.computeVisualLineCount("", 10, TAB8));
        }

        @Test
        void 表示幅0は1行() {
            assertEquals(1, VisualLineUtil.computeVisualLineCount("hello", 0, TAB8));
        }
    }

    @Nested
    class ComputeVisualLineForOffset {

        @Test
        void 折り返しなしの行ではオフセットに関わらず0() {
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("hello", 10, 0, TAB8));
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("hello", 10, 3, TAB8));
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("hello", 10, 5, TAB8));
        }

        @Test
        void 折り返しありの行で各視覚行のオフセットが正しい() {
            // "abcdefgh" を幅3で: abc|def|gh
            // cp0-2 → 視覚行0, cp3-5 → 視覚行1, cp6-7 → 視覚行2
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 0, TAB8));
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 2, TAB8));
            assertEquals(1, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 3, TAB8));
            assertEquals(1, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 5, TAB8));
            assertEquals(2, VisualLineUtil.computeVisualLineForOffset("abcdefgh", 3, 6, TAB8));
        }

        @Test
        void 空文字列は0() {
            assertEquals(0, VisualLineUtil.computeVisualLineForOffset("", 10, 0, TAB8));
        }
    }

    @Nested
    class VisualLineStartOffset {

        @Test
        void 視覚行0の開始オフセットは0() {
            assertEquals(0, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 0, TAB8));
        }

        @Test
        void 折り返しの各視覚行の開始オフセット() {
            // "abcdefgh" を幅3で: abc|def|gh → breaks at cp3, cp6
            assertEquals(0, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 0, TAB8));
            assertEquals(3, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 1, TAB8));
            assertEquals(6, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 2, TAB8));
        }

        @Test
        void 範囲外の視覚行番号は末尾を返す() {
            assertEquals(8, VisualLineUtil.visualLineStartOffset("abcdefgh", 3, 10, TAB8));
        }
    }

    @Nested
    class VisualLineEndOffset {

        @Test
        void 折り返しなしの行の終了オフセットは文字数() {
            assertEquals(5, VisualLineUtil.visualLineEndOffset("hello", 10, 0, TAB8));
        }

        @Test
        void 折り返しの各視覚行の終了オフセット() {
            // "abcdefgh" を幅3で: abc|def|gh → breaks at cp3, cp6
            assertEquals(3, VisualLineUtil.visualLineEndOffset("abcdefgh", 3, 0, TAB8));
            assertEquals(6, VisualLineUtil.visualLineEndOffset("abcdefgh", 3, 1, TAB8));
            assertEquals(8, VisualLineUtil.visualLineEndOffset("abcdefgh", 3, 2, TAB8));
        }

        @Test
        void 最終視覚行の終了オフセットは文字数() {
            assertEquals(8, VisualLineUtil.visualLineEndOffset("abcdefgh", 3, 2, TAB8));
        }
    }
}
