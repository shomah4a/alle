package io.github.shomah4a.alle.libs.ringbuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArrayRingBufferTest {

    @Nested
    class コンストラクタ {

        @Test
        void 容量1以上で作成できる() {
            var buffer = new ArrayRingBuffer<String>(1);
            assertEquals(1, buffer.capacity());
            assertEquals(0, buffer.size());
            assertTrue(buffer.isEmpty());
        }

        @Test
        void 容量0で例外が発生する() {
            assertThrows(IllegalArgumentException.class, () -> new ArrayRingBuffer<String>(0));
        }

        @Test
        void 負の容量で例外が発生する() {
            assertThrows(IllegalArgumentException.class, () -> new ArrayRingBuffer<String>(-1));
        }
    }

    @Nested
    class 要素の追加と取得 {

        @Test
        void 要素を追加して取得できる() {
            var buffer = new ArrayRingBuffer<String>(3);
            buffer.add("a");
            buffer.add("b");
            assertEquals(2, buffer.size());
            assertEquals("a", buffer.get(0));
            assertEquals("b", buffer.get(1));
        }

        @Test
        void 容量いっぱいまで追加できる() {
            var buffer = new ArrayRingBuffer<String>(3);
            buffer.add("a");
            buffer.add("b");
            buffer.add("c");
            assertEquals(3, buffer.size());
            assertEquals("a", buffer.get(0));
            assertEquals("b", buffer.get(1));
            assertEquals("c", buffer.get(2));
        }

        @Test
        void 範囲外のインデックスで例外が発生する() {
            var buffer = new ArrayRingBuffer<String>(3);
            buffer.add("a");
            assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(-1));
            assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(1));
        }

        @Test
        void 空のバッファでgetすると例外が発生する() {
            var buffer = new ArrayRingBuffer<String>(3);
            assertThrows(IndexOutOfBoundsException.class, () -> buffer.get(0));
        }
    }

    @Nested
    class 容量超過時の上書き {

        @Test
        void 容量を超えると最古の要素が上書きされる() {
            var buffer = new ArrayRingBuffer<String>(3);
            buffer.add("a");
            buffer.add("b");
            buffer.add("c");
            buffer.add("d");
            assertEquals(3, buffer.size());
            assertEquals("b", buffer.get(0));
            assertEquals("c", buffer.get(1));
            assertEquals("d", buffer.get(2));
        }

        @Test
        void 容量1で常に最新の要素のみ保持する() {
            var buffer = new ArrayRingBuffer<String>(1);
            buffer.add("a");
            assertEquals("a", buffer.get(0));
            buffer.add("b");
            assertEquals(1, buffer.size());
            assertEquals("b", buffer.get(0));
        }

        @Test
        void 容量の2倍以上追加しても正しく動作する() {
            var buffer = new ArrayRingBuffer<Integer>(3);
            for (int i = 0; i < 10; i++) {
                buffer.add(i);
            }
            assertEquals(3, buffer.size());
            assertEquals(7, buffer.get(0));
            assertEquals(8, buffer.get(1));
            assertEquals(9, buffer.get(2));
        }
    }

    @Nested
    class クリア {

        @Test
        void クリアするとサイズが0になる() {
            var buffer = new ArrayRingBuffer<String>(3);
            buffer.add("a");
            buffer.add("b");
            buffer.clear();
            assertEquals(0, buffer.size());
            assertTrue(buffer.isEmpty());
        }

        @Test
        void クリア後に再度追加できる() {
            var buffer = new ArrayRingBuffer<String>(3);
            buffer.add("a");
            buffer.add("b");
            buffer.clear();
            buffer.add("c");
            assertEquals(1, buffer.size());
            assertEquals("c", buffer.get(0));
        }
    }

    @Nested
    class イテレータ {

        @Test
        void 全要素を順序通りにイテレートできる() {
            var buffer = new ArrayRingBuffer<String>(5);
            buffer.add("a");
            buffer.add("b");
            buffer.add("c");

            MutableList<String> result = Lists.mutable.empty();
            for (String s : buffer) {
                result.add(s);
            }
            assertEquals(Lists.mutable.of("a", "b", "c"), result);
        }

        @Test
        void 上書き後も正しい順序でイテレートできる() {
            var buffer = new ArrayRingBuffer<String>(3);
            buffer.add("a");
            buffer.add("b");
            buffer.add("c");
            buffer.add("d");
            buffer.add("e");

            MutableList<String> result = Lists.mutable.empty();
            for (String s : buffer) {
                result.add(s);
            }
            assertEquals(Lists.mutable.of("c", "d", "e"), result);
        }

        @Test
        void 空のバッファのイテレータはhasNextがfalse() {
            var buffer = new ArrayRingBuffer<String>(3);
            var it = buffer.iterator();
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
        }
    }
}
