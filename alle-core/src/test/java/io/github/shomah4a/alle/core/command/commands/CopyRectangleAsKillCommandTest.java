package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.RectangleKillRing;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class CopyRectangleAsKillCommandTest {

    @Test
    void コマンド名はcopyRectangleAsKillである() {
        assertEquals("copy-rectangle-as-kill", new CopyRectangleAsKillCommand(new RectangleKillRing()).name());
    }

    @Test
    void 矩形をキルリングに保存してバッファは変更しない() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\nbaz\n");
        window.setMark(0);
        window.setPoint(10); // "baz" の "z"の後の"\n"直後、つまり行4の先頭ではなく "az" の手前

        // 行0:foo / 行1:bar / 行2:baz / 行3:空
        // mark=0 (行0 col 0), point=10 (行2 col 2 → "baz" の "z" の後)
        window.setMark(0);
        window.setPoint(10);
        new CopyRectangleAsKillCommand(ring).execute(context).join();

        assertEquals("foo\nbar\nbaz\n", window.getBuffer().getText());
        var saved = ring.current().orElseThrow();
        assertEquals(Lists.immutable.of("fo", "ba", "ba"), saved);
    }

    @Test
    void markが未設定の場合は何もしない() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\n");

        new CopyRectangleAsKillCommand(ring).execute(context).join();

        assertEquals("foo\nbar\n", window.getBuffer().getText());
        assertTrue(ring.current().isEmpty());
    }

    @Test
    void 全角境界を跨ぐ矩形は跨ぎ文字を含めて保存() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        // "aあb" と "xyz" の 2 行
        window.insert("aあb\nxyz\n");
        window.setMark(0);
        // 行1 (xyz) の col=2 → offset = (4 + 2) = 6
        window.setPoint(6);
        new CopyRectangleAsKillCommand(ring).execute(context).join();

        var saved = ring.current().orElseThrow();
        // 行0 "aあb" の col[0, 2) → 右境界 col 2 はあの中央。E 案ではあを含めて "aあ"
        // 行1 "xyz" の col[0, 2) → "xy"
        assertEquals(Lists.immutable.of("aあ", "xy"), saved);
    }
}
