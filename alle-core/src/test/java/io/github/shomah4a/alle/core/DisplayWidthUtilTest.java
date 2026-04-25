package io.github.shomah4a.alle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DisplayWidthUtilTest {

    private static final int TAB8 = 8;

    @Test
    void ASCII文字の表示幅は1() {
        assertEquals(1, DisplayWidthUtil.getDisplayWidth('a', 0, TAB8));
        assertEquals(1, DisplayWidthUtil.getDisplayWidth('Z', 0, TAB8));
        assertEquals(1, DisplayWidthUtil.getDisplayWidth(' ', 0, TAB8));
    }

    @Test
    void CJK漢字の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('漢', 0, TAB8));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('字', 0, TAB8));
    }

    @Test
    void ひらがなの表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('あ', 0, TAB8));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('ん', 0, TAB8));
    }

    @Test
    void カタカナの表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('ア', 0, TAB8));
    }

    @Test
    void 全角英数字の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('Ａ', 0, TAB8));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('１', 0, TAB8));
    }

    @Test
    void 半角カタカナの表示幅は1() {
        assertEquals(1, DisplayWidthUtil.getDisplayWidth('ｱ', 0, TAB8));
    }

    @Test
    void CJK句読点の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('。', 0, TAB8));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('、', 0, TAB8));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('「', 0, TAB8));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('」', 0, TAB8));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('〜', 0, TAB8));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('々', 0, TAB8));
    }

    @Test
    void CJK互換形の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('︵', 0, TAB8));
    }

    @Test
    void 囲みCJK文字の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('㈱', 0, TAB8));
    }

    @Test
    void カタカナ拡張の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('ㇰ', 0, TAB8));
    }

    @Test
    void タブは次のタブストップまでの幅() {
        // カラム0のタブは8カラム進む
        assertEquals(8, DisplayWidthUtil.getDisplayWidth('\t', 0, TAB8));
        // カラム1のタブはタブストップ8まで残り7カラム進む
        assertEquals(7, DisplayWidthUtil.getDisplayWidth('\t', 1, TAB8));
        // カラム3のタブは残り5カラム
        assertEquals(5, DisplayWidthUtil.getDisplayWidth('\t', 3, TAB8));
        // カラム7のタブは残り1カラム
        assertEquals(1, DisplayWidthUtil.getDisplayWidth('\t', 7, TAB8));
        // カラム8のタブは次のタブストップ16まで8カラム
        assertEquals(8, DisplayWidthUtil.getDisplayWidth('\t', 8, TAB8));
        // カラム9のタブは残り7カラム
        assertEquals(7, DisplayWidthUtil.getDisplayWidth('\t', 9, TAB8));
    }

    @Test
    void タブ幅4を指定した場合は4カラム境界でストップ() {
        assertEquals(4, DisplayWidthUtil.getDisplayWidth('\t', 0, 4));
        assertEquals(3, DisplayWidthUtil.getDisplayWidth('\t', 1, 4));
        assertEquals(1, DisplayWidthUtil.getDisplayWidth('\t', 3, 4));
        assertEquals(4, DisplayWidthUtil.getDisplayWidth('\t', 4, 4));
    }

    @Test
    void isFullWidthでCJK文字はtrue() {
        assertTrue(DisplayWidthUtil.isFullWidth('漢'));
        assertTrue(DisplayWidthUtil.isFullWidth('あ'));
    }

    @Test
    void isFullWidthでCJK句読点はtrue() {
        assertTrue(DisplayWidthUtil.isFullWidth('。'));
        assertTrue(DisplayWidthUtil.isFullWidth('「'));
    }

    @Test
    void isFullWidthでASCII文字はfalse() {
        assertFalse(DisplayWidthUtil.isFullWidth('a'));
        assertFalse(DisplayWidthUtil.isFullWidth('1'));
    }

    @Test
    void isFullWidthでU2E80未満の非ASCII文字はfalse() {
        // ラテン拡張 (U+00C0)
        assertFalse(DisplayWidthUtil.isFullWidth(0x00C0));
        // アラビア文字 (U+0627)
        assertFalse(DisplayWidthUtil.isFullWidth(0x0627));
        // U+2E7F (CJK Radicals Supplement の直前)
        assertFalse(DisplayWidthUtil.isFullWidth(0x2E7F));
    }

    @Test
    void isFullWidthでCJK部首補助ブロックはtrue() {
        // U+2E80 (CJK Radicals Supplement の先頭、早期リターン閾値の境界)
        assertTrue(DisplayWidthUtil.isFullWidth(0x2E80));
    }

    @Test
    void computeColumnForOffsetでASCII文字列のカラム計算() {
        assertEquals(0, DisplayWidthUtil.computeColumnForOffset("hello", 0, TAB8));
        assertEquals(3, DisplayWidthUtil.computeColumnForOffset("hello", 3, TAB8));
        assertEquals(5, DisplayWidthUtil.computeColumnForOffset("hello", 5, TAB8));
    }

    @Test
    void computeColumnForOffsetで全角混在文字列のカラム計算() {
        // "aあb" → a(1) + あ(2) + b(1)
        String text = "aあb";
        assertEquals(0, DisplayWidthUtil.computeColumnForOffset(text, 0, TAB8));
        assertEquals(1, DisplayWidthUtil.computeColumnForOffset(text, 1, TAB8)); // aの後
        assertEquals(3, DisplayWidthUtil.computeColumnForOffset(text, 2, TAB8)); // あの後
        assertEquals(4, DisplayWidthUtil.computeColumnForOffset(text, 3, TAB8)); // bの後
    }

    @Test
    void computeColumnForOffsetでタブ混在文字列のカラム計算() {
        // "a\tb" → a(col0→1) + tab(col1→8) + b(col8→9)
        String text = "a\tb";
        assertEquals(0, DisplayWidthUtil.computeColumnForOffset(text, 0, TAB8));
        assertEquals(1, DisplayWidthUtil.computeColumnForOffset(text, 1, TAB8));
        assertEquals(8, DisplayWidthUtil.computeColumnForOffset(text, 2, TAB8));
        assertEquals(9, DisplayWidthUtil.computeColumnForOffset(text, 3, TAB8));
    }

    @Test
    void computeColumnForOffsetで先頭タブはタブストップ幅() {
        // "\tfoo" tab=8 → tab(col0→8)
        assertEquals(0, DisplayWidthUtil.computeColumnForOffset("\tfoo", 0, TAB8));
        assertEquals(8, DisplayWidthUtil.computeColumnForOffset("\tfoo", 1, TAB8));
        assertEquals(9, DisplayWidthUtil.computeColumnForOffset("\tfoo", 2, TAB8));
    }

    @Test
    void computeColumnForOffsetでオフセットが文字数を超える場合は末尾カラムを返す() {
        assertEquals(2, DisplayWidthUtil.computeColumnForOffset("ab", 10, TAB8));
    }

    @Test
    void computeColumnForOffsetで空文字列() {
        assertEquals(0, DisplayWidthUtil.computeColumnForOffset("", 0, TAB8));
    }

    @Test
    void snapColumnToCharBoundaryで文字境界上はそのまま返す() {
        // "aあb" → a(col0), あ(col1-2), b(col3)
        assertEquals(0, DisplayWidthUtil.snapColumnToCharBoundary("aあb", 0, TAB8));
        assertEquals(1, DisplayWidthUtil.snapColumnToCharBoundary("aあb", 1, TAB8));
        assertEquals(3, DisplayWidthUtil.snapColumnToCharBoundary("aあb", 3, TAB8));
    }

    @Test
    void snapColumnToCharBoundaryで全角文字途中は先頭カラムに丸める() {
        // "aあb" → あは col1-2、column=2 は あの途中 → col1 に丸め
        assertEquals(1, DisplayWidthUtil.snapColumnToCharBoundary("aあb", 2, TAB8));
    }

    @Test
    void snapColumnToCharBoundaryで全角文字のみの場合() {
        // "あい" → あ(col0-1), い(col2-3)
        assertEquals(0, DisplayWidthUtil.snapColumnToCharBoundary("あい", 0, TAB8));
        assertEquals(0, DisplayWidthUtil.snapColumnToCharBoundary("あい", 1, TAB8)); // あの途中
        assertEquals(2, DisplayWidthUtil.snapColumnToCharBoundary("あい", 2, TAB8));
        assertEquals(2, DisplayWidthUtil.snapColumnToCharBoundary("あい", 3, TAB8)); // いの途中
    }

    @Test
    void snapColumnToCharBoundaryでタブ途中はタブ先頭カラムに丸める() {
        // "\tx" → tab(col0-7), x(col8)
        assertEquals(0, DisplayWidthUtil.snapColumnToCharBoundary("\tx", 0, TAB8));
        assertEquals(0, DisplayWidthUtil.snapColumnToCharBoundary("\tx", 1, TAB8)); // tabの途中
        assertEquals(0, DisplayWidthUtil.snapColumnToCharBoundary("\tx", 7, TAB8)); // tabの途中
        assertEquals(8, DisplayWidthUtil.snapColumnToCharBoundary("\tx", 8, TAB8));
        assertEquals(9, DisplayWidthUtil.snapColumnToCharBoundary("\tx", 9, TAB8));
    }

    @Test
    void snapColumnToCharBoundaryでテキスト末尾を超える場合() {
        assertEquals(2, DisplayWidthUtil.snapColumnToCharBoundary("ab", 5, TAB8));
    }

    @Test
    void snapColumnToCharBoundaryで負の値は0を返す() {
        assertEquals(0, DisplayWidthUtil.snapColumnToCharBoundary("abc", -1, TAB8));
    }

    @Test
    void snapColumnToCharBoundaryで空文字列() {
        assertEquals(0, DisplayWidthUtil.snapColumnToCharBoundary("", 3, TAB8));
    }

    @Test
    void computeColumnWidthInRangeでASCII文字範囲() {
        // "abcdef" の cp[1,4) → b, c, d の3カラム
        assertEquals(3, DisplayWidthUtil.computeColumnWidthInRange("abcdef", 1, 4, TAB8));
    }

    @Test
    void computeColumnWidthInRangeは範囲startCpを0として計算する() {
        // "abc\tX" の cp[3,5) は tab と X。視覚行ローカル基準では
        // tab@col0 = 幅8、X@col8 = 幅1 → 合計9
        assertEquals(9, DisplayWidthUtil.computeColumnWidthInRange("abc\tX", 3, 5, TAB8));
    }

    @Test
    void computeColumnWidthInRangeで同じstartCpとendCpは0() {
        assertEquals(0, DisplayWidthUtil.computeColumnWidthInRange("abc", 1, 1, TAB8));
    }

    @Test
    void computeColumnWidthInRangeで全角文字範囲() {
        // "aあb" の cp[1,2) は あ → 2カラム
        assertEquals(2, DisplayWidthUtil.computeColumnWidthInRange("aあb", 1, 2, TAB8));
    }

    @Test
    void computeOffsetForColumnでASCII文字列の目的カラムに到達() {
        // "abcdef" で targetColumn=3 → cp=3
        assertEquals(3, DisplayWidthUtil.computeOffsetForColumn("abcdef", 0, 6, 3, TAB8));
    }

    @Test
    void computeOffsetForColumnで全角文字途中は手前で止まる() {
        // "aあb" で targetColumn=2 → あ の途中 → cp=1（あを超えない）
        assertEquals(1, DisplayWidthUtil.computeOffsetForColumn("aあb", 0, 3, 2, TAB8));
    }

    @Test
    void computeOffsetForColumnで全角文字境界に到達() {
        // "aあb" で targetColumn=3 → あ の後 → cp=2
        assertEquals(2, DisplayWidthUtil.computeOffsetForColumn("aあb", 0, 3, 3, TAB8));
    }

    @Test
    void computeOffsetForColumnでタブ途中は手前で止まる() {
        // "a\tb" で targetColumn=4 → タブ途中 → cp=1（タブを超えない）
        assertEquals(1, DisplayWidthUtil.computeOffsetForColumn("a\tb", 0, 3, 4, TAB8));
    }

    @Test
    void computeOffsetForColumnで目的カラムが範囲を超える場合はendCpを返す() {
        assertEquals(6, DisplayWidthUtil.computeOffsetForColumn("abcdef", 0, 6, 100, TAB8));
    }

    @Test
    void computeOffsetForColumnでstartCp基準でカラムを計算する() {
        // "abc\tX" の cp[3,5) は tab と X
        // startCp=3 を 0 基準として targetColumn=8 → tab の後 → cp=4
        assertEquals(4, DisplayWidthUtil.computeOffsetForColumn("abc\tX", 3, 5, 8, TAB8));
        // targetColumn=9 → X の後 → cp=5
        assertEquals(5, DisplayWidthUtil.computeOffsetForColumn("abc\tX", 3, 5, 9, TAB8));
        // targetColumn=7 → tab の途中 → cp=3（tab を超えない）
        assertEquals(3, DisplayWidthUtil.computeOffsetForColumn("abc\tX", 3, 5, 7, TAB8));
    }

    @Test
    void computeOffsetForColumnで空文字列は0を返す() {
        assertEquals(0, DisplayWidthUtil.computeOffsetForColumn("", 0, 0, 5, TAB8));
    }

    @Test
    void computeOffsetForColumnでtargetColumn0は開始位置を返す() {
        assertEquals(2, DisplayWidthUtil.computeOffsetForColumn("abcdef", 2, 6, 0, TAB8));
    }
}
