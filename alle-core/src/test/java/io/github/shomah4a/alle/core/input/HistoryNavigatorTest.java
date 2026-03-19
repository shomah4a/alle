package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class HistoryNavigatorTest {

    @Nested
    class previous {

        @Test
        void 履歴が空の場合はemptyを返す() {
            var history = new InputHistory();
            var navigator = new HistoryNavigator(history, "original");

            assertTrue(navigator.previous().isEmpty());
        }

        @Test
        void 最新の履歴から順に返す() {
            var history = new InputHistory();
            history.add("/home/a.txt");
            history.add("/home/b.txt");
            history.add("/home/c.txt");
            var navigator = new HistoryNavigator(history, "original");

            assertEquals("/home/c.txt", navigator.previous().orElseThrow());
            assertEquals("/home/b.txt", navigator.previous().orElseThrow());
            assertEquals("/home/a.txt", navigator.previous().orElseThrow());
        }

        @Test
        void 先頭に達するとemptyを返す() {
            var history = new InputHistory();
            history.add("/home/a.txt");
            var navigator = new HistoryNavigator(history, "original");

            navigator.previous(); // /home/a.txt
            assertTrue(navigator.previous().isEmpty());
        }
    }

    @Nested
    class next {

        @Test
        void 初期状態ではemptyを返す() {
            var history = new InputHistory();
            history.add("/home/a.txt");
            var navigator = new HistoryNavigator(history, "original");

            assertTrue(navigator.next().isEmpty());
        }

        @Test
        void previousの後にnextで次の履歴に進む() {
            var history = new InputHistory();
            history.add("/home/a.txt");
            history.add("/home/b.txt");
            history.add("/home/c.txt");
            var navigator = new HistoryNavigator(history, "original");

            navigator.previous(); // c
            navigator.previous(); // b
            assertEquals("/home/c.txt", navigator.next().orElseThrow());
        }

        @Test
        void 末尾を超えると元入力を返す() {
            var history = new InputHistory();
            history.add("/home/a.txt");
            var navigator = new HistoryNavigator(history, "original");

            navigator.previous(); // a
            assertEquals("original", navigator.next().orElseThrow());
        }

        @Test
        void 元入力位置でnextを呼ぶとemptyを返す() {
            var history = new InputHistory();
            history.add("/home/a.txt");
            var navigator = new HistoryNavigator(history, "original");

            navigator.previous(); // a
            navigator.next(); // original
            assertTrue(navigator.next().isEmpty());
        }
    }

    @Nested
    class previousとnextの組み合わせ {

        @Test
        void 前後に移動して正しい履歴を返す() {
            var history = new InputHistory();
            history.add("/home/a.txt");
            history.add("/home/b.txt");
            history.add("/home/c.txt");
            var navigator = new HistoryNavigator(history, "original");

            assertEquals("/home/c.txt", navigator.previous().orElseThrow());
            assertEquals("/home/b.txt", navigator.previous().orElseThrow());
            assertEquals("/home/c.txt", navigator.next().orElseThrow());
            assertEquals("/home/b.txt", navigator.previous().orElseThrow());
            assertEquals("/home/a.txt", navigator.previous().orElseThrow());
            assertTrue(navigator.previous().isEmpty());
            assertEquals("/home/b.txt", navigator.next().orElseThrow());
        }
    }
}
