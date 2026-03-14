package io.github.shomah4a.alle.core.keybind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.EnumSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KeyStrokeTest {

    @Nested
    class 生成 {

        @Test
        void 修飾キーなしのキーストロークを生成できる() {
            var ks = KeyStroke.of('a');
            assertEquals('a', ks.keyCode());
            assertEquals(EnumSet.noneOf(Modifier.class), ks.modifiers());
        }

        @Test
        void Ctrl付きのキーストロークを生成できる() {
            var ks = KeyStroke.ctrl('x');
            assertEquals('x', ks.keyCode());
            assertEquals(EnumSet.of(Modifier.CTRL), ks.modifiers());
        }

        @Test
        void Meta付きのキーストロークを生成できる() {
            var ks = KeyStroke.meta('f');
            assertEquals('f', ks.keyCode());
            assertEquals(EnumSet.of(Modifier.META), ks.modifiers());
        }

        @Test
        void 複数の修飾キーを指定できる() {
            var ks = KeyStroke.of('a', Modifier.CTRL, Modifier.SHIFT);
            assertEquals(EnumSet.of(Modifier.CTRL, Modifier.SHIFT), ks.modifiers());
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
}
