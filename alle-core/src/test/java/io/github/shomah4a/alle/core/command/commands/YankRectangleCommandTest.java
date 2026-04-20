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

        // point は kill 後に矩形左上(offset 0)に移動しているはず
        new YankRectangleCommand(ring).execute(context).join();

        // 行0 col 0 に "ab" 挿入 → "abcd\n"
        // 行1 col 0 に "ef" 挿入 → "efgh\n"
        // 行2 col 0 に "ij" 挿入 → "ijkl\n"
        assertEquals("abcd\nefgh\nijkl\n", window.getBuffer().getText());
    }

    // === マルチバイト境界 ===

    @Test
    void point挿入位置がタブ中央なら文字の後ろに挿入される() {
        // point は cp 単位でしか指せないため、タブ中央のカラム位置は直接表現できない。
        // 代わりに「同一行の別カラムで yank したとき、タブを破壊せず後ろに挿入する」
        // ことを insertAtColumnRightSnap 経由で検証する。
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("a\tb\n");

        // col 3 (タブ中央) に "XY" を挿入 → 右スナップで col 8 (タブの後)
        RectangleGeometry.insertAtColumnRightSnap(window.getBuffer(), 0, 3, "XY", 8);

        assertEquals("a\tXYb\n", window.getBuffer().getText());
    }

    @Test
    void insertAtColumnRightSnapで全角中央は全角の後ろに挿入() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("aあb\n");

        // col 2 (あの中央) に "XY" → あの後ろ (col 3) に挿入 → "aあXYb"
        RectangleGeometry.insertAtColumnRightSnap(window.getBuffer(), 0, 2, "XY", 8);

        assertEquals("aあXYb\n", window.getBuffer().getText());
    }

    @Test
    void 全角の前のカラムにyankすると全角の手前に挿入される() {
        var ring = new RectangleKillRing();
        ring.put(Lists.immutable.of("XY"));
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("aあb\n");
        window.setPoint(1); // col 1 (あの直前、境界上)

        new YankRectangleCommand(ring).execute(context).join();

        // col 1 → あの手前 → "aXYあb"
        assertEquals("aXYあb\n", window.getBuffer().getText());
    }

    @Test
    void 行がpoint列に届かない場合は末尾paddingしてからyank() {
        var ring = new RectangleKillRing();
        ring.put(Lists.immutable.of("XY", "ZW"));
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("abcde\n");
        window.setPoint(5); // 行0 col 5 (末尾)

        new YankRectangleCommand(ring).execute(context).join();

        // 行0 col 5 に "XY" → "abcdeXY"
        // 行1（存在しない）→ 改行追加して行1 col 5 に "ZW" → 空行に padding 5 + "ZW" → "     ZW"
        assertEquals("abcdeXY\n     ZW", window.getBuffer().getText());
    }
}
