package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WindowTest {

    private Buffer createBuffer() {
        return new Buffer("test", new GapTextModel());
    }

    private Window createWindow() {
        return new Window(createBuffer());
    }

    @Nested
    class 初期状態 {

        @Test
        void バッファを保持しポイントは0() {
            var window = createWindow();
            assertEquals("test", window.getBuffer().getName());
            assertEquals(0, window.getPoint());
            assertEquals(0, window.getDisplayStartLine());
        }
    }

    @Nested
    class カーソル位置での挿入 {

        @Test
        void カーソル位置に文字列を挿入してカーソルが後ろに移動する() {
            var window = createWindow();
            window.insert("Hello");
            assertEquals("Hello", window.getBuffer().getText());
            assertEquals(5, window.getPoint());
            assertTrue(window.getBuffer().isDirty());
        }

        @Test
        void 連続挿入でカーソル位置が進む() {
            var window = createWindow();
            window.insert("Hello");
            window.insert(" World");
            assertEquals("Hello World", window.getBuffer().getText());
            assertEquals(11, window.getPoint());
        }

        @Test
        void 絵文字を挿入してもカーソル位置がコードポイント単位で正しい() {
            var window = createWindow();
            window.insert("A😀B");
            assertEquals(3, window.getPoint());
        }
    }

    @Nested
    class バックスペースとデリート {

        @Test
        void バックスペースでカーソル手前の文字を削除する() {
            var window = createWindow();
            window.insert("Hello");
            window.deleteBackward(1);
            assertEquals("Hell", window.getBuffer().getText());
            assertEquals(4, window.getPoint());
        }

        @Test
        void 先頭でバックスペースしても何も起きない() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(0);
            window.deleteBackward(1);
            assertEquals("Hello", window.getBuffer().getText());
            assertEquals(0, window.getPoint());
        }

        @Test
        void デリートでカーソル後ろの文字を削除する() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(0);
            window.deleteForward(1);
            assertEquals("ello", window.getBuffer().getText());
            assertEquals(0, window.getPoint());
        }

        @Test
        void 末尾でデリートしても何も起きない() {
            var window = createWindow();
            window.insert("Hello");
            window.deleteForward(1);
            assertEquals("Hello", window.getBuffer().getText());
            assertEquals(5, window.getPoint());
        }
    }

    @Nested
    class ポイント設定 {

        @Test
        void 範囲内のポイントを設定できる() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(3);
            assertEquals(3, window.getPoint());
        }

        @Test
        void 末尾位置にポイントを設定できる() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(5);
            assertEquals(5, window.getPoint());
        }

        @Test
        void 範囲外のポイント設定で例外が発生する() {
            var window = createWindow();
            window.insert("Hello");
            assertThrows(IndexOutOfBoundsException.class, () -> window.setPoint(6));
            assertThrows(IndexOutOfBoundsException.class, () -> window.setPoint(-1));
        }
    }

    @Nested
    class 表示開始行 {

        @Test
        void 表示開始行を設定できる() {
            var window = createWindow();
            window.insert("Hello\nWorld\nFoo");
            window.setDisplayStartLine(1);
            assertEquals(1, window.getDisplayStartLine());
        }

        @Test
        void 範囲外の行で例外が発生する() {
            var window = createWindow();
            window.insert("Hello\nWorld");
            assertThrows(IndexOutOfBoundsException.class, () -> window.setDisplayStartLine(2));
            assertThrows(IndexOutOfBoundsException.class, () -> window.setDisplayStartLine(-1));
        }
    }

    @Nested
    class バッファ切り替え {

        @Test
        void バッファを切り替えるとポイントと表示開始行がリセットされる() {
            var window = createWindow();
            window.insert("Hello");
            window.setPoint(3);

            var newBuffer = new Buffer("other", new GapTextModel());
            window.setBuffer(newBuffer);

            assertEquals("other", window.getBuffer().getName());
            assertEquals(0, window.getPoint());
            assertEquals(0, window.getDisplayStartLine());
        }
    }

    @Nested
    class 直前バッファ {

        @Test
        void 初期状態では直前バッファがない() {
            var window = createWindow();
            assertTrue(window.getPreviousBuffer().isEmpty());
        }

        @Test
        void バッファ切り替え後に直前バッファが記録される() {
            var window = createWindow();
            var originalBuffer = window.getBuffer();
            var newBuffer = new Buffer("new", new GapTextModel());

            window.setBuffer(newBuffer);

            assertTrue(window.getPreviousBuffer().isPresent());
            assertSame(originalBuffer, window.getPreviousBuffer().get());
        }

        @Test
        void 複数回切り替えると直近の切り替え元が直前バッファになる() {
            var window = createWindow();
            var bufferB = new Buffer("b", new GapTextModel());
            var bufferC = new Buffer("c", new GapTextModel());

            window.setBuffer(bufferB);
            window.setBuffer(bufferC);

            assertSame(bufferB, window.getPreviousBuffer().get());
        }
    }

    @Nested
    class 同一バッファの複数ウィンドウ {

        @Test
        void 同じバッファを持つ複数ウィンドウが独立したポイントを持てる() {
            var buffer = createBuffer();
            var window1 = new Window(buffer);
            var window2 = new Window(buffer);

            window1.insert("Hello World");
            window1.setPoint(5);
            window2.setPoint(0);

            assertEquals(5, window1.getPoint());
            assertEquals(0, window2.getPoint());
        }

        @Test
        void 片方のウィンドウでの編集がもう片方のバッファにも反映される() {
            var buffer = createBuffer();
            var window1 = new Window(buffer);
            var window2 = new Window(buffer);

            window1.insert("Hello");
            assertEquals("Hello", window2.getBuffer().getText());
        }

        @Test
        void 片方のウィンドウでの削除でもう片方のポイントがバッファ長を超過した場合clampされる() {
            var buffer = createBuffer();
            var window1 = new Window(buffer);
            var window2 = new Window(buffer);

            window1.insert("Hello");
            window2.setPoint(5);
            // window1で全削除するとバッファ長が0になる
            window1.setPoint(0);
            window1.deleteForward(5);

            assertEquals(0, window2.getPoint());
        }
    }
}
