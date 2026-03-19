package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class InputHistoryTest {

    private InputHistory history;

    @BeforeEach
    void setUp() {
        history = new InputHistory();
    }

    @Nested
    class add {

        @Test
        void 追加した入力が履歴に含まれる() {
            history.add("/home/test.txt");

            assertEquals(1, history.size());
            assertEquals("/home/test.txt", history.get(0));
        }

        @Test
        void 複数の入力が追加順に保持される() {
            history.add("/home/a.txt");
            history.add("/home/b.txt");
            history.add("/home/c.txt");

            assertEquals(3, history.size());
            assertEquals("/home/a.txt", history.get(0));
            assertEquals("/home/b.txt", history.get(1));
            assertEquals("/home/c.txt", history.get(2));
        }

        @Test
        void 空文字列は追加されない() {
            history.add("");

            assertEquals(0, history.size());
        }

        @Test
        void 重複する入力は最新位置に移動する() {
            history.add("/home/a.txt");
            history.add("/home/b.txt");
            history.add("/home/a.txt");

            assertEquals(2, history.size());
            assertEquals("/home/b.txt", history.get(0));
            assertEquals("/home/a.txt", history.get(1));
        }

        @Test
        void 最大サイズを超えると古い入力が削除される() {
            var smallHistory = new InputHistory(3);
            smallHistory.add("a");
            smallHistory.add("b");
            smallHistory.add("c");
            smallHistory.add("d");

            assertEquals(3, smallHistory.size());
            assertEquals("b", smallHistory.get(0));
            assertEquals("c", smallHistory.get(1));
            assertEquals("d", smallHistory.get(2));
        }
    }

    @Nested
    class size {

        @Test
        void 空の履歴のサイズは0() {
            assertEquals(0, history.size());
        }
    }
}
