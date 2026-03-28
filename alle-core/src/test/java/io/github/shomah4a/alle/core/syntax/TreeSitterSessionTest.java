package io.github.shomah4a.alle.core.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TreeSitterSessionTest {

    @Test
    void 末尾に文字を追加した場合のオフセットが正しい() {
        // "abc" → "abcd": 共通prefix=3, 共通suffix=0
        var edit = TreeSitterSession.computeInputEdit("abc", "abcd");
        assertEquals(3, edit.getStartByte());
        assertEquals(3, edit.getOldEndByte());
        assertEquals(4, edit.getNewEndByte());
    }

    @Test
    void 先頭に文字を挿入した場合のオフセットが正しい() {
        // "bc" → "abc": 共通prefix=0, 共通suffix=2
        var edit = TreeSitterSession.computeInputEdit("bc", "abc");
        assertEquals(0, edit.getStartByte());
        assertEquals(0, edit.getOldEndByte());
        assertEquals(1, edit.getNewEndByte());
    }

    @Test
    void 中間の文字を置換した場合のオフセットが正しい() {
        // "abc" → "axc": 共通prefix=1, 共通suffix=1
        var edit = TreeSitterSession.computeInputEdit("abc", "axc");
        assertEquals(1, edit.getStartByte());
        assertEquals(2, edit.getOldEndByte());
        assertEquals(2, edit.getNewEndByte());
    }

    @Test
    void 文字を削除した場合のオフセットが正しい() {
        // "abcd" → "ad": 共通prefix=1, 共通suffix=1
        var edit = TreeSitterSession.computeInputEdit("abcd", "ad");
        assertEquals(1, edit.getStartByte());
        assertEquals(3, edit.getOldEndByte());
        assertEquals(1, edit.getNewEndByte());
    }

    @Test
    void 日本語テキストでUTF8バイトオフセットが正しい() {
        // "あいう" → "あえう": 共通prefix=1(あ), 共通suffix=1(う)
        var edit = TreeSitterSession.computeInputEdit("あいう", "あえう");
        assertEquals(3, edit.getStartByte());
        assertEquals(6, edit.getOldEndByte());
        assertEquals(6, edit.getNewEndByte());
    }

    @Test
    void 改行を含むテキストでTSPointが正しい() {
        // "ab\ncd" → "ab\nXcd": 共通prefix=3(a,b,\n), 共通suffix=2(c,d)
        var edit = TreeSitterSession.computeInputEdit("ab\ncd", "ab\nXcd");
        assertEquals(1, edit.getStartPoint().getRow());
        assertEquals(0, edit.getStartPoint().getColumn());
    }

    @Test
    void テキスト全体が置換された場合のオフセットが正しい() {
        var edit = TreeSitterSession.computeInputEdit("abc", "xyz");
        assertEquals(0, edit.getStartByte());
        assertEquals(3, edit.getOldEndByte());
        assertEquals(3, edit.getNewEndByte());
    }
}
