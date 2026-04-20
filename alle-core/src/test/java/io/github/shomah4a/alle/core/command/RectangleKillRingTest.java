package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class RectangleKillRingTest {

    @Test
    void 未保存の場合はempty() {
        var ring = new RectangleKillRing();
        assertTrue(ring.current().isEmpty());
    }

    @Test
    void putした矩形がcurrentで取れる() {
        var ring = new RectangleKillRing();
        ring.put(Lists.immutable.of("foo", "bar", "baz"));
        assertEquals(Lists.immutable.of("foo", "bar", "baz"), ring.current().orElseThrow());
    }

    @Test
    void putは前のエントリを上書きする() {
        var ring = new RectangleKillRing();
        ring.put(Lists.immutable.of("a", "b"));
        ring.put(Lists.immutable.of("c", "d", "e"));
        assertEquals(Lists.immutable.of("c", "d", "e"), ring.current().orElseThrow());
    }

    @Test
    void 空リストもput可能() {
        var ring = new RectangleKillRing();
        ring.put(Lists.immutable.empty());
        assertTrue(ring.current().orElseThrow().isEmpty());
    }
}
