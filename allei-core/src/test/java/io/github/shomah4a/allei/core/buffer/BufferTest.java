package io.github.shomah4a.allei.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.allei.core.textmodel.GapTextModel;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferTest {

    private Buffer createBuffer() {
        return new Buffer("test", new GapTextModel());
    }

    @Nested
    class 初期状態 {

        @Test
        void 名前とテキストモデルを保持する() {
            var buffer = createBuffer();
            assertEquals("test", buffer.getName());
            assertEquals(0, buffer.getTextModel().length());
        }

        @Test
        void 初期状態はクリーンでポイントは0() {
            var buffer = createBuffer();
            assertFalse(buffer.isDirty());
            assertEquals(0, buffer.getPoint());
        }

        @Test
        void ファイルパスはオプショナル() {
            var buffer = createBuffer();
            assertTrue(buffer.getFilePath().isEmpty());

            var bufferWithPath = new Buffer("test", new GapTextModel(), Path.of("/tmp/test.txt"));
            assertEquals(Path.of("/tmp/test.txt"), bufferWithPath.getFilePath().orElseThrow());
        }
    }

    @Nested
    class カーソル位置での挿入 {

        @Test
        void カーソル位置に文字列を挿入してカーソルが後ろに移動する() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            assertEquals("Hello", buffer.getText());
            assertEquals(5, buffer.getPoint());
            assertTrue(buffer.isDirty());
        }

        @Test
        void 連続挿入でカーソル位置が進む() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            buffer.insert(" World");
            assertEquals("Hello World", buffer.getText());
            assertEquals(11, buffer.getPoint());
        }

        @Test
        void 絵文字を挿入してもカーソル位置がコードポイント単位で正しい() {
            var buffer = createBuffer();
            buffer.insert("A😀B");
            assertEquals(3, buffer.getPoint());
        }
    }

    @Nested
    class 指定位置への挿入 {

        @Test
        void カーソル以前の位置に挿入するとカーソルが調整される() {
            var buffer = createBuffer();
            buffer.insert("World");
            // point = 5
            buffer.insertAt(0, "Hello ");
            assertEquals("Hello World", buffer.getText());
            assertEquals(11, buffer.getPoint());
        }

        @Test
        void カーソル以降の位置に挿入してもカーソルは変わらない() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            buffer.setPoint(0);
            buffer.insertAt(5, " World");
            assertEquals("Hello World", buffer.getText());
            assertEquals(0, buffer.getPoint());
        }
    }

    @Nested
    class 削除 {

        @Test
        void 指定位置から削除してカーソルが調整される() {
            var buffer = createBuffer();
            buffer.insert("Hello World");
            // point = 11
            buffer.deleteAt(0, 6);
            assertEquals("World", buffer.getText());
            assertEquals(5, buffer.getPoint());
        }

        @Test
        void 削除範囲にカーソルが含まれる場合は削除開始位置に移動する() {
            var buffer = createBuffer();
            buffer.insert("Hello World");
            buffer.setPoint(3);
            buffer.deleteAt(1, 5);
            assertEquals("Hello World".substring(0, 1) + "Hello World".substring(6), buffer.getText());
            assertEquals(1, buffer.getPoint());
        }

        @Test
        void カーソル以降の削除ではカーソルは変わらない() {
            var buffer = createBuffer();
            buffer.insert("Hello World");
            buffer.setPoint(5);
            buffer.deleteAt(6, 5);
            assertEquals("Hello ", buffer.getText());
            assertEquals(5, buffer.getPoint());
        }
    }

    @Nested
    class バックスペースとデリート {

        @Test
        void バックスペースでカーソル手前の文字を削除する() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            buffer.deleteBackward(1);
            assertEquals("Hell", buffer.getText());
            assertEquals(4, buffer.getPoint());
        }

        @Test
        void 先頭でバックスペースしても何も起きない() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            buffer.setPoint(0);
            buffer.deleteBackward(1);
            assertEquals("Hello", buffer.getText());
            assertEquals(0, buffer.getPoint());
            // dirtyはfalseのまま（insertで一度trueになるが、ここではdeleteBackwardでは変更なし）
        }

        @Test
        void デリートでカーソル後ろの文字を削除する() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            buffer.setPoint(0);
            buffer.deleteForward(1);
            assertEquals("ello", buffer.getText());
            assertEquals(0, buffer.getPoint());
        }

        @Test
        void 末尾でデリートしても何も起きない() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            buffer.deleteForward(1);
            assertEquals("Hello", buffer.getText());
            assertEquals(5, buffer.getPoint());
        }
    }

    @Nested
    class ポイント設定 {

        @Test
        void 範囲内のポイントを設定できる() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            buffer.setPoint(3);
            assertEquals(3, buffer.getPoint());
        }

        @Test
        void 末尾位置にポイントを設定できる() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            buffer.setPoint(5);
            assertEquals(5, buffer.getPoint());
        }

        @Test
        void 範囲外のポイント設定で例外が発生する() {
            var buffer = createBuffer();
            buffer.insert("Hello");
            assertThrows(IndexOutOfBoundsException.class, () -> buffer.setPoint(6));
            assertThrows(IndexOutOfBoundsException.class, () -> buffer.setPoint(-1));
        }
    }

    @Nested
    class ダーティフラグ {

        @Test
        void 挿入でダーティになる() {
            var buffer = createBuffer();
            assertFalse(buffer.isDirty());
            buffer.insert("a");
            assertTrue(buffer.isDirty());
        }

        @Test
        void markCleanでクリーンに戻る() {
            var buffer = createBuffer();
            buffer.insert("a");
            assertTrue(buffer.isDirty());
            buffer.markClean();
            assertFalse(buffer.isDirty());
        }
    }
}
