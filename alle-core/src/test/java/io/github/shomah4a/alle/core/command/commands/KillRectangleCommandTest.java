package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.command.RectangleKillRing;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class KillRectangleCommandTest {

    @Test
    void コマンド名はkillRectangleである() {
        assertEquals("kill-rectangle", new KillRectangleCommand(new RectangleKillRing()).name());
    }

    @Test
    void 矩形範囲を削除してRingに保存する() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\nbaz\n");
        window.setMark(0);
        window.setPoint(10);

        new KillRectangleCommand(ring).execute(context).join();

        assertEquals("o\nr\nz\n", window.getBuffer().getText());
        assertEquals(Lists.immutable.of("fo", "ba", "ba"), ring.current().orElseThrow());
    }
}
