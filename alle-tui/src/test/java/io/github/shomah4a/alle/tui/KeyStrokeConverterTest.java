package io.github.shomah4a.alle.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.googlecode.lanterna.input.KeyType;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Modifier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KeyStrokeConverterTest {

    @Nested
    class 文字キー変換 {

        @Test
        void ASCII文字を変換する() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke('a', false, false);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isPresent());
            assertEquals(KeyStroke.of('a'), result.get());
        }

        @Test
        void 大文字を変換する() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke('A', false, false);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isPresent());
            assertEquals(KeyStroke.of('A'), result.get());
        }

        @Test
        void スペースを変換する() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke(' ', false, false);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isPresent());
            assertEquals(KeyStroke.of(' '), result.get());
        }
    }

    @Nested
    class 修飾キー変換 {

        @Test
        void Ctrl付き文字を変換する() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke('f', true, false);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isPresent());
            assertEquals(KeyStroke.ctrl('f'), result.get());
        }

        @Test
        void Alt付き文字をMeta修飾に変換する() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke('x', false, true);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isPresent());
            assertEquals(KeyStroke.meta('x'), result.get());
        }

        @Test
        void CtrlとAlt両方の修飾キーを変換する() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke('a', true, true);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isPresent());
            assertEquals(KeyStroke.of('a', Modifier.CTRL, Modifier.META), result.get());
        }
    }

    @Nested
    class 特殊キー変換 {

        @Test
        void EnterキーをLFに変換する() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke(KeyType.Enter);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isPresent());
            assertEquals(KeyStroke.of('\n'), result.get());
        }

        @Test
        void Backspaceキーを0x7Fに変換する() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke(KeyType.Backspace);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isPresent());
            assertEquals(KeyStroke.of(0x7F), result.get());
        }

        @Test
        void EOFはemptyを返す() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke(KeyType.EOF);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isEmpty());
        }

        @Test
        void 未対応の特殊キーはemptyを返す() {
            var lanterna = new com.googlecode.lanterna.input.KeyStroke(KeyType.ArrowUp);
            var result = KeyStrokeConverter.convert(lanterna);

            assertTrue(result.isEmpty());
        }
    }
}
