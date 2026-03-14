package io.github.shomah4a.alle.core.keybind;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KeySequenceTest {

    @Nested
    class 単一キー {

        @Test
        void 単一キーストロークのシーケンスを生成できる() {
            var seq = KeySequence.of(KeyStroke.ctrl('f'));
            assertEquals(1, seq.length());
            assertEquals(KeyStroke.ctrl('f'), seq.first());
        }
    }

    @Nested
    class 複合キー {

        @Test
        void 複数キーストロークのシーケンスを生成できる() {
            var seq = KeySequence.of(KeyStroke.ctrl('x'), KeyStroke.ctrl('s'));
            assertEquals(2, seq.length());
            assertEquals(KeyStroke.ctrl('x'), seq.first());
        }

        @Test
        void 先頭を除いた残りを取得できる() {
            var seq = KeySequence.of(KeyStroke.ctrl('x'), KeyStroke.ctrl('s'));
            var rest = seq.rest();
            assertEquals(1, rest.length());
            assertEquals(KeyStroke.ctrl('s'), rest.first());
        }
    }

    @Nested
    class 等価性 {

        @Test
        void 同じキーシーケンスは等しい() {
            var seq1 = KeySequence.of(KeyStroke.ctrl('x'), KeyStroke.ctrl('s'));
            var seq2 = KeySequence.of(KeyStroke.ctrl('x'), KeyStroke.ctrl('s'));
            assertEquals(seq1, seq2);
        }
    }
}
