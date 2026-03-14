package io.github.shomah4a.allei.libs.gapbuffer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GapModelTest {

    @Nested
    class コンストラクタ {

        @Test
        void デフォルトコンストラクタで空のバッファが生成される() {
            var model = new GapModel();
            assertEquals(0, model.length());
            assertEquals("", model.toString());
        }

        @Test
        void 初期ギャップサイズを指定して生成できる() {
            var model = new GapModel(128);
            assertEquals(0, model.length());
        }

        @Test
        void 負のギャップサイズを指定すると例外が発生する() {
            assertThrows(IllegalArgumentException.class, () -> new GapModel(-1));
        }
    }

    @Nested
    class insert {

        @Test
        void 空バッファの先頭に文字列を挿入できる() {
            var model = new GapModel();
            model.insert(0, "Hello");
            assertEquals("Hello", model.toString());
            assertEquals(5, model.length());
        }

        @Test
        void 末尾に文字列を追加できる() {
            var model = new GapModel();
            model.insert(0, "Hello");
            model.insert(5, " World");
            assertEquals("Hello World", model.toString());
        }

        @Test
        void 中間に文字列を挿入できる() {
            var model = new GapModel();
            model.insert(0, "HWorld");
            model.insert(1, "ello ");
            assertEquals("Hello World", model.toString());
        }

        @Test
        void 空文字列の挿入は何もしない() {
            var model = new GapModel();
            model.insert(0, "Hello");
            model.insert(2, "");
            assertEquals("Hello", model.toString());
        }

        @Test
        void 一文字ずつ挿入できる() {
            var model = new GapModel();
            model.insert(0, 'A');
            model.insert(1, 'B');
            model.insert(2, 'C');
            assertEquals("ABC", model.toString());
        }

        @Test
        void 範囲外のオフセットで例外が発生する() {
            var model = new GapModel();
            model.insert(0, "AB");
            assertThrows(IndexOutOfBoundsException.class, () -> model.insert(3, "C"));
            assertThrows(IndexOutOfBoundsException.class, () -> model.insert(-1, "C"));
        }

        @Test
        void ギャップサイズを超える大量テキストを挿入できる() {
            var model = new GapModel(4);
            var longText = "A".repeat(10000);
            model.insert(0, longText);
            assertEquals(10000, model.length());
            assertEquals(longText, model.toString());
        }
    }

    @Nested
    class delete {

        @Test
        void 先頭から削除できる() {
            var model = new GapModel();
            model.insert(0, "Hello World");
            model.delete(0, 6);
            assertEquals("World", model.toString());
        }

        @Test
        void 末尾から削除できる() {
            var model = new GapModel();
            model.insert(0, "Hello World");
            model.delete(5, 6);
            assertEquals("Hello", model.toString());
        }

        @Test
        void 中間を削除できる() {
            var model = new GapModel();
            model.insert(0, "Hello World");
            model.delete(5, 1);
            assertEquals("HelloWorld", model.toString());
        }

        @Test
        void 全文を削除すると空になる() {
            var model = new GapModel();
            model.insert(0, "Hello");
            model.delete(0, 5);
            assertEquals(0, model.length());
            assertEquals("", model.toString());
        }

        @Test
        void カウント0の削除は何もしない() {
            var model = new GapModel();
            model.insert(0, "Hello");
            model.delete(2, 0);
            assertEquals("Hello", model.toString());
        }

        @Test
        void 範囲外の削除で例外が発生する() {
            var model = new GapModel();
            model.insert(0, "Hello");
            assertThrows(IndexOutOfBoundsException.class, () -> model.delete(3, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> model.delete(-1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> model.delete(0, -1));
        }

        @Test
        void 空バッファから削除すると例外が発生する() {
            var model = new GapModel();
            assertThrows(IndexOutOfBoundsException.class, () -> model.delete(0, 1));
        }
    }

    @Nested
    class charAt {

        @Test
        void 各位置の文字を取得できる() {
            var model = new GapModel();
            model.insert(0, "ABC");
            assertEquals('A', model.charAt(0));
            assertEquals('B', model.charAt(1));
            assertEquals('C', model.charAt(2));
        }

        @Test
        void 挿入と削除の後でも正しい文字を取得できる() {
            var model = new GapModel();
            model.insert(0, "ABCDE");
            model.delete(1, 2);
            assertEquals('A', model.charAt(0));
            assertEquals('D', model.charAt(1));
            assertEquals('E', model.charAt(2));
        }

        @Test
        void 範囲外のオフセットで例外が発生する() {
            var model = new GapModel();
            model.insert(0, "AB");
            assertThrows(IndexOutOfBoundsException.class, () -> model.charAt(2));
            assertThrows(IndexOutOfBoundsException.class, () -> model.charAt(-1));
        }

        @Test
        void 空バッファで例外が発生する() {
            var model = new GapModel();
            assertThrows(IndexOutOfBoundsException.class, () -> model.charAt(0));
        }
    }

    @Nested
    class substring {

        @Test
        void 部分文字列を取得できる() {
            var model = new GapModel();
            model.insert(0, "Hello World");
            assertEquals("Hello", model.substring(0, 5));
            assertEquals("World", model.substring(6, 11));
            assertEquals("lo Wo", model.substring(3, 8));
        }

        @Test
        void 全体を取得できる() {
            var model = new GapModel();
            model.insert(0, "Hello");
            assertEquals("Hello", model.substring(0, 5));
        }

        @Test
        void 空範囲は空文字列を返す() {
            var model = new GapModel();
            model.insert(0, "Hello");
            assertEquals("", model.substring(2, 2));
        }

        @Test
        void ギャップをまたぐ部分文字列を取得できる() {
            var model = new GapModel(4);
            model.insert(0, "ABCDE");
            model.insert(2, "XY");
            // "ABXYCDE"
            assertEquals("BXYC", model.substring(1, 5));
        }

        @Test
        void 範囲外で例外が発生する() {
            var model = new GapModel();
            model.insert(0, "Hello");
            assertThrows(IndexOutOfBoundsException.class, () -> model.substring(-1, 3));
            assertThrows(IndexOutOfBoundsException.class, () -> model.substring(3, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> model.substring(0, 6));
        }
    }

    @Nested
    class ギャップ操作 {

        @Test
        void ギャップサイズ0の直後に挿入できる() {
            var model = new GapModel(3);
            model.insert(0, "ABC");
            // ギャップサイズが0になっているはず
            model.insert(3, "D");
            assertEquals("ABCD", model.toString());
        }

        @Test
        void 連続して異なる位置に挿入と削除を繰り返せる() {
            var model = new GapModel(4);
            model.insert(0, "ABCDEF");
            model.delete(1, 1);   // "ACDEF"
            model.insert(3, "X"); // "ACDXEF"
            model.delete(0, 1);   // "CDXEF"
            model.insert(5, "G"); // "CDXEFG"
            assertEquals("CDXEFG", model.toString());
            assertEquals(6, model.length());
        }
    }
}
