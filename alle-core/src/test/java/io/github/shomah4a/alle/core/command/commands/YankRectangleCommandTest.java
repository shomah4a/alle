package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.command.RectangleKillRing;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class YankRectangleCommandTest {

    @Test
    void コマンド名はyankRectangleである() {
        assertEquals("yank-rectangle", new YankRectangleCommand(new RectangleKillRing()).name());
    }

    @Test
    void 保存された矩形がpoint位置に挿入される() {
        var ring = new RectangleKillRing();
        ring.put(Lists.immutable.of("XY", "ZW"));
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("abcd\nefgh\n");
        window.setPoint(1); // 行0 col 1

        new YankRectangleCommand(ring).execute(context).join();

        // 行0 col 1 に "XY" を挿入 → "aXYbcd"
        // 行1 col 1 に "ZW" を挿入 → "eZWfgh"
        assertEquals("aXYbcd\neZWfgh\n", window.getBuffer().getText());
    }

    @Test
    void Ringが空の場合は何もしない() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("abc\n");

        new YankRectangleCommand(ring).execute(context).join();

        assertEquals("abc\n", window.getBuffer().getText());
    }

    @Test
    void バッファ末尾を超える行数は改行を追加して挿入() {
        var ring = new RectangleKillRing();
        ring.put(Lists.immutable.of("XY", "ZW", "PQ"));
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("ab");
        window.setPoint(1);

        new YankRectangleCommand(ring).execute(context).join();

        // 行0 col 1 に "XY" 挿入 → "aXYb"
        // 行1（存在しない）→ 改行追加して "XYb\n" → 行1 col 1 に "ZW"
        //   → 行1 は空文字列 "" だったので col 1 まで padding → " ZW"
        // 行2（存在しない）→ 改行追加して → 行2 col 1 に "PQ" → " PQ"
        assertEquals("aXYb\n ZW\n PQ", window.getBuffer().getText());
    }

    @Test
    void 矩形往復kill_yankでバッファが復元される() {
        var ring = new RectangleKillRing();
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("abcd\nefgh\nijkl\n");
        window.setMark(0);
        window.setPoint(12); // 行2 col 2

        new KillRectangleCommand(ring).execute(context).join();
        // 削除後: "cd\ngh\nkl\n"
        assertEquals("cd\ngh\nkl\n", window.getBuffer().getText());

        // point を先頭に戻して yank
        window.setPoint(0);
        new YankRectangleCommand(ring).execute(context).join();

        // 行0 col 0 に "ab" 挿入 → "abcd\n"
        // 行1 col 0 に "ef" 挿入 → "efgh\n"
        // 行2 col 0 に "ij" 挿入 → "ijkl\n"
        assertEquals("abcd\nefgh\nijkl\n", window.getBuffer().getText());
    }
}
