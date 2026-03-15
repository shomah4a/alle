package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KillRingTest {

    @Nested
    class 初期状態 {

        @Test
        void 空のkillRingのcurrentはempty() {
            var ring = new KillRing();
            assertTrue(ring.current().isEmpty());
        }

        @Test
        void 空のkillRingのsizeは0() {
            var ring = new KillRing();
            assertEquals(0, ring.size());
        }
    }

    @Nested
    class push {

        @Test
        void pushしたテキストがcurrentになる() {
            var ring = new KillRing();
            ring.push("hello");
            assertEquals("hello", ring.current().orElseThrow());
        }

        @Test
        void 複数pushすると最新がcurrentになる() {
            var ring = new KillRing();
            ring.push("first");
            ring.push("second");
            assertEquals("second", ring.current().orElseThrow());
            assertEquals(2, ring.size());
        }

        @Test
        void 最大サイズを超えると古いエントリが削除される() {
            var ring = new KillRing(3);
            ring.push("a");
            ring.push("b");
            ring.push("c");
            ring.push("d");
            assertEquals(3, ring.size());
            assertEquals("d", ring.current().orElseThrow());
        }
    }

    @Nested
    class appendToLast {

        @Test
        void 最新エントリに追記される() {
            var ring = new KillRing();
            ring.push("hello");
            ring.appendToLast(" world");
            assertEquals("hello world", ring.current().orElseThrow());
            assertEquals(1, ring.size());
        }

        @Test
        void 空のkillRingにappendToLastするとpush相当になる() {
            var ring = new KillRing();
            ring.appendToLast("hello");
            assertEquals("hello", ring.current().orElseThrow());
            assertEquals(1, ring.size());
        }

        @Test
        void 連続appendToLastで結合される() {
            var ring = new KillRing();
            ring.push("a");
            ring.appendToLast("b");
            ring.appendToLast("c");
            assertEquals("abc", ring.current().orElseThrow());
        }
    }
}
