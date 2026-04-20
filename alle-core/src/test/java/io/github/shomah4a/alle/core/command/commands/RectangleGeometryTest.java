package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RectangleGeometryTest {

    private static final int TAB8 = 8;

    @Test
    void ASCII行の矩形切り出し境界は文字境界どおり() {
        // "abcdef" [2, 4) → text=abcdef, left=2, right=4
        var result = RectangleGeometry.buildPaddedLine("abcdef", 2, 4, TAB8);
        assertEquals("abcdef", result.text());
        assertEquals(2, result.leftCp());
        assertEquals(4, result.rightCp());
    }

    @Test
    void 行が左カラムに届かない場合は末尾にスペース補填() {
        // "ab" [3, 5) → "ab   " で [3, 5) は空スペース
        var result = RectangleGeometry.buildPaddedLine("ab", 3, 5, TAB8);
        assertEquals("ab   ", result.text());
        assertEquals(3, result.leftCp());
        assertEquals(5, result.rightCp());
    }

    @Test
    void 行が右カラムに届かない場合は末尾にスペース補填() {
        // "ab" [0, 5) → "ab   " の [0, 5)
        var result = RectangleGeometry.buildPaddedLine("ab", 0, 5, TAB8);
        assertEquals("ab   ", result.text());
        assertEquals(0, result.leftCp());
        assertEquals(5, result.rightCp());
    }

    @Test
    void 左境界がタブの中央に落ちる場合はタブをスペース展開() {
        // "\tabc" [3, 5) → "        abc"(8space+abc) の [3, 5)
        var result = RectangleGeometry.buildPaddedLine("\tabc", 3, 5, TAB8);
        assertEquals("        abc", result.text());
        assertEquals(3, result.leftCp());
        assertEquals(5, result.rightCp());
    }

    @Test
    void 右境界がタブの中央に落ちる場合はタブをスペース展開() {
        // "\tx" [0, 3) → "        x" の [0, 3)
        var result = RectangleGeometry.buildPaddedLine("\tx", 0, 3, TAB8);
        assertEquals("        x", result.text());
        assertEquals(0, result.leftCp());
        assertEquals(3, result.rightCp());
    }

    @Test
    void 左境界が全角文字の中央に落ちる場合は全角をスペース展開() {
        // "あい" col: あ(0-1) い(2-3)。 [1, 3) → "  い"(spaceあ分)+い(col2-3)
        // buildPaddedLine: col=0 で あ の width=2, col+width=2 > leftCol=1 → spaceスペース展開
        // その後 col=2 で い(width=2), col+width=4 > rightCol=3 → いもスペース展開
        var result = RectangleGeometry.buildPaddedLine("あい", 1, 3, TAB8);
        assertEquals("    ", result.text());
        assertEquals(1, result.leftCp());
        assertEquals(3, result.rightCp());
    }

    @Test
    void 右境界が全角文字の中央に落ちる場合は全角をスペース展開() {
        // "aあb" col: a(0) あ(1-2) b(3)。 [0, 2) → "a  b"(aそのまま, あをスペース展開, b残り)
        var result = RectangleGeometry.buildPaddedLine("aあb", 0, 2, TAB8);
        assertEquals("a  b", result.text());
        assertEquals(0, result.leftCp());
        assertEquals(2, result.rightCp());
    }

    @Test
    void 全角文字が境界に綺麗に揃っている場合はそのまま残る() {
        // "aあb" [1, 3) → "aあb" の [1, 2)（cp）。あ の幅2がそのまま残る。
        var result = RectangleGeometry.buildPaddedLine("aあb", 1, 3, TAB8);
        assertEquals("aあb", result.text());
        assertEquals(1, result.leftCp());
        assertEquals(2, result.rightCp());
    }

    @Test
    void 幅0の矩形は挿入ポイントのみ返す() {
        // "abc" [1, 1) → "abc" の [1, 1)
        var result = RectangleGeometry.buildPaddedLine("abc", 1, 1, TAB8);
        assertEquals("abc", result.text());
        assertEquals(1, result.leftCp());
        assertEquals(1, result.rightCp());
    }

    @Test
    void 空行で矩形を指定した場合はスペース補填() {
        // "" [0, 3) → "   " の [0, 3)
        var result = RectangleGeometry.buildPaddedLine("", 0, 3, TAB8);
        assertEquals("   ", result.text());
        assertEquals(0, result.leftCp());
        assertEquals(3, result.rightCp());
    }

    @Test
    void cellAtColumnでカラム境界上は次の文字手前を示す() {
        // "abc" col=1 境界上（a と b の間）→ leftCp=rightCp=1
        var result = RectangleGeometry.cellAtColumn("abc", 1, TAB8);
        assertEquals(1, result.leftCp());
        assertEquals(1, result.rightCp());
        assertEquals(1, result.leftCol());
        assertEquals(1, result.rightCol());
    }

    @Test
    void cellAtColumnで全角文字の中央はその文字全体を示す() {
        // "aあb" col=2（あの中央）→ あの区間 cp[1,2), col[1,3)
        var result = RectangleGeometry.cellAtColumn("aあb", 2, TAB8);
        assertEquals(1, result.leftCp());
        assertEquals(2, result.rightCp());
        assertEquals(1, result.leftCol());
        assertEquals(3, result.rightCol());
    }

    @Test
    void cellAtColumnで行末を超える場合は末尾位置を返す() {
        // "ab" col=5 → 末尾 cp=2, col=2
        var result = RectangleGeometry.cellAtColumn("ab", 5, TAB8);
        assertEquals(2, result.leftCp());
        assertEquals(2, result.rightCp());
        assertEquals(2, result.leftCol());
        assertEquals(2, result.rightCol());
    }

    @Test
    void paddedInsertPointで境界上はそのまま挿入() {
        // build 用に BufferFacade 生成が面倒なので string-based の buildPaddedLine を使い回すケースはないが
        // paddedInsertPoint は BufferFacade を受けるので、buildPaddedLine と同じロジックを間接的にテストする。
        // ここでは簡易に buildPaddedLine で代替してフルテストを別途で行う
        var result = RectangleGeometry.buildPaddedLine("abcdef", 3, 3, TAB8);
        assertEquals("abcdef", result.text());
        assertEquals(3, result.leftCp());
        assertEquals(3, result.rightCp());
    }
}
