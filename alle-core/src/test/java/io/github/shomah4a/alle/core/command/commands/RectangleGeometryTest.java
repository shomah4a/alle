package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RectangleGeometryTest {

    private static final int TAB8 = 8;

    // === columnRoundDown ===

    @Test
    void columnRoundDownでASCII境界上はそのままのcp() {
        // "abcdef" col 3 → cp 3
        assertEquals(3, RectangleGeometry.columnRoundDown("abcdef", 3, TAB8));
    }

    @Test
    void columnRoundDownで全角中央は手前の文字境界() {
        // "aあb": a(col0) あ(col1-2) b(col3). target=2 はあの中央 → cp 1 (aの後、あの手前)
        assertEquals(1, RectangleGeometry.columnRoundDown("aあb", 2, TAB8));
    }

    @Test
    void columnRoundDownで全角境界上() {
        // "aあb" col 3 → cp 2 (あの後、bの手前)
        assertEquals(2, RectangleGeometry.columnRoundDown("aあb", 3, TAB8));
    }

    @Test
    void columnRoundDownでタブ中央は手前の文字境界() {
        // "a\tb": a(col0) \t(col1-7) b(col8). target=3 はタブ中央 → cp 1 (aの後、タブの手前)
        assertEquals(1, RectangleGeometry.columnRoundDown("a\tb", 3, TAB8));
    }

    @Test
    void columnRoundDownで行末を超える場合は行末のcp() {
        assertEquals(3, RectangleGeometry.columnRoundDown("abc", 10, TAB8));
    }

    // === columnRoundUp ===

    @Test
    void columnRoundUpでASCII境界上はそのままのcp() {
        assertEquals(3, RectangleGeometry.columnRoundUp("abcdef", 3, TAB8));
    }

    @Test
    void columnRoundUpで全角中央は後ろの文字境界() {
        // "aあb" target=2 はあの中央 → cp 2 (あの後)
        assertEquals(2, RectangleGeometry.columnRoundUp("aあb", 2, TAB8));
    }

    @Test
    void columnRoundUpでタブ中央は後ろの文字境界() {
        // "a\tb" target=3 はタブ中央 → cp 2 (タブの後、bの手前)
        assertEquals(2, RectangleGeometry.columnRoundUp("a\tb", 3, TAB8));
    }

    @Test
    void columnRoundUpで行末を超える場合は行末のcp() {
        assertEquals(3, RectangleGeometry.columnRoundUp("abc", 10, TAB8));
    }
}
