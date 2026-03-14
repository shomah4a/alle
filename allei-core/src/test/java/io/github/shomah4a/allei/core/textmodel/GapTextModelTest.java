package io.github.shomah4a.allei.core.textmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GapTextModelTest {

    private TextModel create() {
        return new GapTextModel();
    }

    @Nested
    class 基本操作 {

        @Test
        void 空の状態で長さが0() {
            var model = create();
            assertEquals(0, model.length());
            assertEquals("", model.getText());
        }

        @Test
        void ASCII文字列を挿入して取得できる() {
            var model = create();
            model.insert(0, "Hello");
            assertEquals(5, model.length());
            assertEquals("Hello", model.getText());
        }

        @Test
        void 中間に挿入できる() {
            var model = create();
            model.insert(0, "HWorld");
            model.insert(1, "ello ");
            assertEquals("Hello World", model.getText());
        }

        @Test
        void 削除できる() {
            var model = create();
            model.insert(0, "Hello World");
            model.delete(5, 6);
            assertEquals("Hello", model.getText());
        }

        @Test
        void 部分文字列を取得できる() {
            var model = create();
            model.insert(0, "Hello World");
            assertEquals("Hello", model.substring(0, 5));
            assertEquals("World", model.substring(6, 11));
        }
    }

    @Nested
    class サロゲートペア対応 {

        @Test
        void 絵文字を挿入して長さがコードポイント単位になる() {
            var model = create();
            model.insert(0, "A😀B");
            assertEquals(3, model.length());
        }

        @Test
        void 絵文字のコードポイントを取得できる() {
            var model = create();
            model.insert(0, "A😀B");
            assertEquals('A', model.codePointAt(0));
            assertEquals(0x1F600, model.codePointAt(1));
            assertEquals('B', model.codePointAt(2));
        }

        @Test
        void 絵文字の前後に挿入できる() {
            var model = create();
            model.insert(0, "😀");
            model.insert(0, "A");
            model.insert(2, "B");
            assertEquals("A😀B", model.getText());
            assertEquals(3, model.length());
        }

        @Test
        void 絵文字を削除できる() {
            var model = create();
            model.insert(0, "A😀B");
            model.delete(1, 1);
            assertEquals("AB", model.getText());
            assertEquals(2, model.length());
        }

        @Test
        void 絵文字を含む部分文字列を取得できる() {
            var model = create();
            model.insert(0, "A😀B😃C");
            assertEquals("😀B😃", model.substring(1, 4));
        }

        @Test
        void 複数のサロゲートペア文字を連続して扱える() {
            var model = create();
            model.insert(0, "😀😃😄");
            assertEquals(3, model.length());
            assertEquals(0x1F600, model.codePointAt(0));
            assertEquals(0x1F603, model.codePointAt(1));
            assertEquals(0x1F604, model.codePointAt(2));
        }

        @Test
        void サロゲートペアの中間位置には挿入されない() {
            var model = create();
            model.insert(0, "😀😃");
            model.insert(1, "X");
            assertEquals("😀X😃", model.getText());
            assertEquals(3, model.length());
        }
    }

    @Nested
    class 行操作 {

        @Test
        void 空テキストの行数は1() {
            var model = create();
            assertEquals(1, model.lineCount());
        }

        @Test
        void 改行なしの行数は1() {
            var model = create();
            model.insert(0, "Hello");
            assertEquals(1, model.lineCount());
        }

        @Test
        void 改行を含むテキストの行数を取得できる() {
            var model = create();
            model.insert(0, "Hello\nWorld\n");
            assertEquals(3, model.lineCount());
        }

        @Test
        void 行の先頭オフセットをコードポイント単位で取得できる() {
            var model = create();
            model.insert(0, "Hello\nWorld");
            assertEquals(0, model.lineStartOffset(0));
            assertEquals(6, model.lineStartOffset(1));
        }

        @Test
        void 絵文字を含む行の先頭オフセットがコードポイント単位で正しい() {
            var model = create();
            model.insert(0, "A😀B\nCD");
            // 行0: "A😀B" = 3コードポイント + 改行 = オフセット4が行1の先頭
            assertEquals(0, model.lineStartOffset(0));
            assertEquals(4, model.lineStartOffset(1));
        }

        @Test
        void 行のテキストを取得できる() {
            var model = create();
            model.insert(0, "Hello\nWorld\nFoo");
            assertEquals("Hello", model.lineText(0));
            assertEquals("World", model.lineText(1));
            assertEquals("Foo", model.lineText(2));
        }

        @Test
        void 絵文字を含む行のテキストを取得できる() {
            var model = create();
            model.insert(0, "A😀B\n😃😄");
            assertEquals("A😀B", model.lineText(0));
            assertEquals("😃😄", model.lineText(1));
        }

        @Test
        void 末尾が改行の場合の最終行は空文字列() {
            var model = create();
            model.insert(0, "Hello\n");
            assertEquals(2, model.lineCount());
            assertEquals("Hello", model.lineText(0));
            assertEquals("", model.lineText(1));
        }

        @Test
        void 範囲外の行インデックスで例外が発生する() {
            var model = create();
            model.insert(0, "Hello\nWorld");
            assertThrows(IndexOutOfBoundsException.class, () -> model.lineStartOffset(2));
            assertThrows(IndexOutOfBoundsException.class, () -> model.lineStartOffset(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> model.lineText(2));
        }
    }

    @Nested
    class CJK拡張漢字のサロゲートペア対応 {

        // 𩸽(ほっけ) U+29E3D はサロゲートペアが必要
        private static final String HOKKE = "\uD867\uDE3D";
        // 𠮟(しかる) U+20B9F
        private static final String SHIKARU = "\uD842\uDF9F";

        @Test
        void サロゲートペアが必要なCJK漢字の長さがコードポイント単位になる() {
            var model = create();
            model.insert(0, "魚は" + HOKKE + "が好き");
            assertEquals(6, model.length());
        }

        @Test
        void サロゲートペアが必要なCJK漢字のコードポイントを取得できる() {
            var model = create();
            model.insert(0, HOKKE);
            assertEquals(0x29E3D, model.codePointAt(0));
        }

        @Test
        void サロゲートペアCJK漢字の前後に挿入できる() {
            var model = create();
            model.insert(0, HOKKE);
            model.insert(0, "A");
            model.insert(2, "B");
            assertEquals("A" + HOKKE + "B", model.getText());
            assertEquals(3, model.length());
        }

        @Test
        void サロゲートペアCJK漢字を削除できる() {
            var model = create();
            model.insert(0, "A" + HOKKE + "B");
            model.delete(1, 1);
            assertEquals("AB", model.getText());
        }

        @Test
        void 複数のサロゲートペアCJK漢字を扱える() {
            var model = create();
            model.insert(0, HOKKE + SHIKARU);
            assertEquals(2, model.length());
            assertEquals(0x29E3D, model.codePointAt(0));
            assertEquals(0x20B9F, model.codePointAt(1));
        }
    }

    @Nested
    class 複合絵文字 {

        // 👨‍👩‍👧 = U+1F468 U+200D U+1F469 U+200D U+1F467 (ZWJシーケンス、5コードポイント)
        private static final String FAMILY = "👨\u200D👩\u200D👧";
        // 🇯🇵 = U+1F1EF U+1F1F5 (リージョナルインジケータ、2コードポイント)
        private static final String FLAG_JP = "🇯🇵";
        // 👍🏽 = U+1F44D U+1F3FD (肌色修飾子、2コードポイント)
        private static final String THUMBS_UP = "👍🏽";

        @Test
        void ZWJシーケンスの長さがコードポイント単位で正しい() {
            var model = create();
            model.insert(0, FAMILY);
            assertEquals(5, model.length());
        }

        @Test
        void 国旗絵文字の長さがコードポイント単位で正しい() {
            var model = create();
            model.insert(0, FLAG_JP);
            assertEquals(2, model.length());
        }

        @Test
        void 肌色修飾子付き絵文字の長さがコードポイント単位で正しい() {
            var model = create();
            model.insert(0, THUMBS_UP);
            assertEquals(2, model.length());
        }

        @Test
        void 複合絵文字の前後に挿入できる() {
            var model = create();
            model.insert(0, FAMILY);
            model.insert(0, "A");
            model.insert(model.length(), "B");
            assertEquals("A" + FAMILY + "B", model.getText());
        }

        @Test
        void 複合絵文字を含む部分文字列を取得できる() {
            var model = create();
            model.insert(0, "X" + FLAG_JP + "Y");
            // X(1) + 🇯🇵(2) + Y(1) = 4コードポイント
            assertEquals(FLAG_JP, model.substring(1, 3));
        }
    }

    @Nested
    class 境界条件 {

        @Test
        void 範囲外のインデックスで例外が発生する() {
            var model = create();
            model.insert(0, "AB");
            assertThrows(IndexOutOfBoundsException.class, () -> model.codePointAt(2));
            assertThrows(IndexOutOfBoundsException.class, () -> model.codePointAt(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> model.insert(3, "C"));
            assertThrows(IndexOutOfBoundsException.class, () -> model.delete(1, 2));
        }

        @Test
        void 空バッファでcodePointAtは例外が発生する() {
            var model = create();
            assertThrows(IndexOutOfBoundsException.class, () -> model.codePointAt(0));
        }

        @Test
        void 空文字列の挿入は何もしない() {
            var model = create();
            model.insert(0, "AB");
            model.insert(1, "");
            assertEquals("AB", model.getText());
        }

        @Test
        void カウント0の削除は何もしない() {
            var model = create();
            model.insert(0, "AB");
            model.delete(0, 0);
            assertEquals("AB", model.getText());
        }
    }
}
