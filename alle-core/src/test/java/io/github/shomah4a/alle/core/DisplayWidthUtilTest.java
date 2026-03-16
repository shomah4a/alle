package io.github.shomah4a.alle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DisplayWidthUtilTest {

    @Test
    void ASCII文字の表示幅は1() {
        assertEquals(1, DisplayWidthUtil.getDisplayWidth('a'));
        assertEquals(1, DisplayWidthUtil.getDisplayWidth('Z'));
        assertEquals(1, DisplayWidthUtil.getDisplayWidth(' '));
    }

    @Test
    void CJK漢字の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('漢'));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('字'));
    }

    @Test
    void ひらがなの表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('あ'));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('ん'));
    }

    @Test
    void カタカナの表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('ア'));
    }

    @Test
    void 全角英数字の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('Ａ'));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('１'));
    }

    @Test
    void 半角カタカナの表示幅は1() {
        assertEquals(1, DisplayWidthUtil.getDisplayWidth('ｱ'));
    }

    @Test
    void CJK句読点の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('。'));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('、'));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('「'));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('」'));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('〜'));
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('々'));
    }

    @Test
    void CJK互換形の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('︵'));
    }

    @Test
    void 囲みCJK文字の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('㈱'));
    }

    @Test
    void カタカナ拡張の表示幅は2() {
        assertEquals(2, DisplayWidthUtil.getDisplayWidth('ㇰ'));
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
    void computeColumnForOffsetでASCII文字列のカラム計算() {
        assertEquals(0, DisplayWidthUtil.computeColumnForOffset("hello", 0));
        assertEquals(3, DisplayWidthUtil.computeColumnForOffset("hello", 3));
        assertEquals(5, DisplayWidthUtil.computeColumnForOffset("hello", 5));
    }

    @Test
    void computeColumnForOffsetで全角混在文字列のカラム計算() {
        // "aあb" → a(1) + あ(2) + b(1)
        String text = "aあb";
        assertEquals(0, DisplayWidthUtil.computeColumnForOffset(text, 0));
        assertEquals(1, DisplayWidthUtil.computeColumnForOffset(text, 1)); // aの後
        assertEquals(3, DisplayWidthUtil.computeColumnForOffset(text, 2)); // あの後
        assertEquals(4, DisplayWidthUtil.computeColumnForOffset(text, 3)); // bの後
    }

    @Test
    void computeColumnForOffsetでオフセットが文字数を超える場合は末尾カラムを返す() {
        assertEquals(2, DisplayWidthUtil.computeColumnForOffset("ab", 10));
    }

    @Test
    void computeColumnForOffsetで空文字列() {
        assertEquals(0, DisplayWidthUtil.computeColumnForOffset("", 0));
    }
}
