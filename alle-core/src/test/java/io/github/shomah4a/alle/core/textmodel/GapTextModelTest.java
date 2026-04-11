package io.github.shomah4a.alle.core.textmodel;

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
    class オフセットから行インデックスへの変換 {

        @Test
        void 単一行のオフセットは行0を返す() {
            var model = create();
            model.insert(0, "Hello");
            assertEquals(0, model.lineIndexForOffset(0));
            assertEquals(0, model.lineIndexForOffset(3));
            assertEquals(0, model.lineIndexForOffset(5));
        }

        @Test
        void 複数行で各行のオフセットが対応する行インデックスを返す() {
            var model = create();
            model.insert(0, "Hello\nWorld\nFoo");
            // 行0: Hello (0-4), 改行(5)
            assertEquals(0, model.lineIndexForOffset(0));
            assertEquals(0, model.lineIndexForOffset(4));
            // 改行文字上はその行の末尾として扱う
            assertEquals(0, model.lineIndexForOffset(5));
            // 行1: World (6-10), 改行(11)
            assertEquals(1, model.lineIndexForOffset(6));
            assertEquals(1, model.lineIndexForOffset(10));
            assertEquals(1, model.lineIndexForOffset(11));
            // 行2: Foo (12-14)
            assertEquals(2, model.lineIndexForOffset(12));
            assertEquals(2, model.lineIndexForOffset(15));
        }

        @Test
        void 空テキストのオフセット0は行0を返す() {
            var model = create();
            assertEquals(0, model.lineIndexForOffset(0));
        }

        @Test
        void 末尾が改行の場合のバッファ末尾は最終行を返す() {
            var model = create();
            model.insert(0, "Hello\n");
            // 行0: Hello\n, 行1: ""
            assertEquals(1, model.lineIndexForOffset(6));
        }

        @Test
        void 絵文字を含む行でオフセットがコードポイント単位で解釈される() {
            var model = create();
            model.insert(0, "A😀B\nCD");
            // 行0: A😀B (0-2), 改行(3), 行1: CD (4-5)
            assertEquals(0, model.lineIndexForOffset(2));
            assertEquals(0, model.lineIndexForOffset(3));
            assertEquals(1, model.lineIndexForOffset(4));
        }

        @Test
        void 範囲外のオフセットで例外が発生する() {
            var model = create();
            model.insert(0, "Hello");
            assertThrows(IndexOutOfBoundsException.class, () -> model.lineIndexForOffset(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> model.lineIndexForOffset(6));
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

    @Nested
    class 改行キャッシュ一貫性 {

        @Test
        void 複数改行を含むテキスト挿入後のlineCountとlineTextが一致する() {
            var model = create();
            model.insert(0, "AAA\nBBB\nCCC\nDDD");
            assertEquals(4, model.lineCount());
            assertEquals("AAA", model.lineText(0));
            assertEquals("BBB", model.lineText(1));
            assertEquals("CCC", model.lineText(2));
            assertEquals("DDD", model.lineText(3));
        }

        @Test
        void 複数行にまたがるdelete後のlineCountとlineTextが一致する() {
            var model = create();
            model.insert(0, "AAA\nBBB\nCCC\nDDD");
            // "BBB\nCCC\n" を削除 → "AAA\nDDD"
            model.delete(4, 8);
            assertEquals(2, model.lineCount());
            assertEquals("AAA", model.lineText(0));
            assertEquals("DDD", model.lineText(1));
        }

        @Test
        void insert後にdelete後にinsertしてもキャッシュが整合する() {
            var model = create();
            model.insert(0, "A\nB\nC");
            assertEquals(3, model.lineCount());

            model.delete(2, 2); // "A\nC"
            assertEquals(2, model.lineCount());
            assertEquals("A", model.lineText(0));
            assertEquals("C", model.lineText(1));

            model.insert(2, "X\nY\n"); // "A\nX\nY\nC"
            assertEquals(4, model.lineCount());
            assertEquals("A", model.lineText(0));
            assertEquals("X", model.lineText(1));
            assertEquals("Y", model.lineText(2));
            assertEquals("C", model.lineText(3));
        }

        @Test
        void 連続改行の挿入と削除後もキャッシュが整合する() {
            var model = create();
            model.insert(0, "\n\n\n");
            assertEquals(4, model.lineCount());
            assertEquals("", model.lineText(0));
            assertEquals("", model.lineText(1));
            assertEquals("", model.lineText(2));
            assertEquals("", model.lineText(3));

            model.delete(0, 2); // "\n" が1つ残る
            assertEquals(2, model.lineCount());
            assertEquals("", model.lineText(0));
            assertEquals("", model.lineText(1));
        }

        @Test
        void サロゲートペアと改行が混在するケースでキャッシュが整合する() {
            var model = create();
            model.insert(0, "😀\nA\n😃B");
            assertEquals(3, model.lineCount());
            assertEquals("😀", model.lineText(0));
            assertEquals("A", model.lineText(1));
            assertEquals("😃B", model.lineText(2));

            assertEquals(0, model.lineStartOffset(0));
            assertEquals(2, model.lineStartOffset(1));
            assertEquals(4, model.lineStartOffset(2));
        }

        @Test
        void 全テキスト削除後に再挿入してもキャッシュが整合する() {
            var model = create();
            model.insert(0, "A\nB\nC");
            model.delete(0, model.length());
            assertEquals(1, model.lineCount());
            assertEquals("", model.lineText(0));

            model.insert(0, "X\nY");
            assertEquals(2, model.lineCount());
            assertEquals("X", model.lineText(0));
            assertEquals("Y", model.lineText(1));
        }

        @Test
        void lineIndexForOffsetがキャッシュベースで正しい値を返す() {
            var model = create();
            model.insert(0, "AAA\nBBB\nCCC");
            // AAA(0-2) \n(3) BBB(4-6) \n(7) CCC(8-10)
            assertEquals(0, model.lineIndexForOffset(0));
            assertEquals(0, model.lineIndexForOffset(2));
            assertEquals(0, model.lineIndexForOffset(3)); // 改行上はその行
            assertEquals(1, model.lineIndexForOffset(4));
            assertEquals(1, model.lineIndexForOffset(7)); // 改行上はその行
            assertEquals(2, model.lineIndexForOffset(8));
            assertEquals(2, model.lineIndexForOffset(11)); // 末尾
        }

        @Test
        void lineStartOffsetがキャッシュベースで正しい値を返す() {
            var model = create();
            model.insert(0, "AAA\nBBB\nCCC");
            assertEquals(0, model.lineStartOffset(0));
            assertEquals(4, model.lineStartOffset(1));
            assertEquals(8, model.lineStartOffset(2));
        }

        @Test
        void 行の中間に改行を挿入した場合にキャッシュが整合する() {
            var model = create();
            model.insert(0, "ABCDEF");
            model.insert(3, "\n");
            assertEquals(2, model.lineCount());
            assertEquals("ABC", model.lineText(0));
            assertEquals("DEF", model.lineText(1));
            assertEquals(0, model.lineStartOffset(0));
            assertEquals(4, model.lineStartOffset(1));
        }

        @Test
        void GapModelコンストラクタで既にデータが入っている場合にキャッシュが初期構築される() {
            var gapModel = new io.github.shomah4a.alle.libs.gapbuffer.GapModel();
            gapModel.insert(0, "A\nB\nC");
            var model = new GapTextModel(gapModel);
            assertEquals(3, model.lineCount());
            assertEquals("A", model.lineText(0));
            assertEquals("B", model.lineText(1));
            assertEquals("C", model.lineText(2));
        }
    }
}
