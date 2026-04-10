package io.github.shomah4a.alle.core.keybind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KeyStrokeTest {

    @Nested
    class 生成 {

        @Test
        void 修飾キーなしのキーストロークを生成できる() {
            var ks = KeyStroke.of('a');
            assertEquals('a', ks.keyCode());
            assertEquals(Sets.immutable.empty(), ks.modifiers());
        }

        @Test
        void Ctrl付きのキーストロークを生成できる() {
            var ks = KeyStroke.ctrl('x');
            assertEquals('x', ks.keyCode());
            assertEquals(Sets.immutable.of(Modifier.CTRL), ks.modifiers());
        }

        @Test
        void Meta付きのキーストロークを生成できる() {
            var ks = KeyStroke.meta('f');
            assertEquals('f', ks.keyCode());
            assertEquals(Sets.immutable.of(Modifier.META), ks.modifiers());
        }

        @Test
        void Shift付きのキーストロークを生成できる() {
            var ks = KeyStroke.shift('\t');
            assertEquals('\t', ks.keyCode());
            assertEquals(Sets.immutable.of(Modifier.SHIFT), ks.modifiers());
        }

        @Test
        void 複数の修飾キーを指定できる() {
            var ks = KeyStroke.of('a', Modifier.CTRL, Modifier.SHIFT);
            assertEquals(Sets.immutable.of(Modifier.CTRL, Modifier.SHIFT), ks.modifiers());
        }
    }

    @Nested
    class 等価性 {

        @Test
        void 同じキーストロークは等しい() {
            assertEquals(KeyStroke.ctrl('x'), KeyStroke.ctrl('x'));
        }

        @Test
        void 異なるキーコードは等しくない() {
            assertNotEquals(KeyStroke.ctrl('x'), KeyStroke.ctrl('y'));
        }

        @Test
        void 異なる修飾キーは等しくない() {
            assertNotEquals(KeyStroke.ctrl('x'), KeyStroke.meta('x'));
        }

        @Test
        void 修飾キーの有無で等しくない() {
            assertNotEquals(KeyStroke.of('x'), KeyStroke.ctrl('x'));
        }
    }

    @Nested
    class 表示文字列 {

        @Test
        void 修飾キーなしの通常文字() {
            assertEquals("a", KeyStroke.of('a').displayString());
        }

        @Test
        void Ctrl修飾キー付き() {
            assertEquals("C-x", KeyStroke.ctrl('x').displayString());
        }

        @Test
        void Meta修飾キー付き() {
            assertEquals("M-f", KeyStroke.meta('f').displayString());
        }

        @Test
        void 複数修飾キー付き() {
            assertEquals(
                    "C-S-a", KeyStroke.of('a', Modifier.CTRL, Modifier.SHIFT).displayString());
        }

        @Test
        void スペースキー() {
            assertEquals("SPC", KeyStroke.of(' ').displayString());
        }

        @Test
        void リターンキー() {
            assertEquals("RET", KeyStroke.of('\n').displayString());
        }

        @Test
        void デリートキー() {
            assertEquals("DEL", KeyStroke.of(0x7F).displayString());
        }

        @Test
        void エスケープキー() {
            assertEquals("ESC", KeyStroke.of(0x1B).displayString());
        }

        @Test
        void 矢印キー上() {
            assertEquals("<up>", KeyStroke.of(KeyStroke.ARROW_UP).displayString());
        }

        @Test
        void 矢印キー下() {
            assertEquals("<down>", KeyStroke.of(KeyStroke.ARROW_DOWN).displayString());
        }

        @Test
        void 矢印キー左() {
            assertEquals("<left>", KeyStroke.of(KeyStroke.ARROW_LEFT).displayString());
        }

        @Test
        void 矢印キー右() {
            assertEquals("<right>", KeyStroke.of(KeyStroke.ARROW_RIGHT).displayString());
        }

        @Test
        void PageUpキー() {
            assertEquals("<prior>", KeyStroke.of(KeyStroke.PAGE_UP).displayString());
        }

        @Test
        void PageDownキー() {
            assertEquals("<next>", KeyStroke.of(KeyStroke.PAGE_DOWN).displayString());
        }

        @Test
        void タブキー() {
            assertEquals("TAB", KeyStroke.of('\t').displayString());
        }

        @Test
        void ShiftタブはEmacsのbacktab表記になる() {
            assertEquals("<backtab>", KeyStroke.shift('\t').displayString());
        }

        @Test
        void 制御文字はコード番号で表示される() {
            assertEquals("<1>", KeyStroke.of(0x01).displayString());
        }

        @Test
        void Ctrl修飾付きスペースキー() {
            assertEquals("C-SPC", KeyStroke.ctrl(' ').displayString());
        }
    }
}
