package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import org.junit.jupiter.api.Test;

class ClearRectangleCommandTest {

    @Test
    void コマンド名はclearRectangleである() {
        assertEquals("clear-rectangle", new ClearRectangleCommand().name());
    }

    @Test
    void 矩形範囲をスペースで埋める() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("abcd\nefgh\nijkl\n");
        window.setMark(0);
        // "abcd\nefgh\nijkl\n" offset 12 は行2 col 2 (k の後)
        window.setPoint(12);

        new ClearRectangleCommand().execute(context).join();

        // 各行の col[0, 2) をスペースで埋める
        assertEquals("  cd\n  gh\n  kl\n", window.getBuffer().getText());
    }

    @Test
    void タブを跨ぐ矩形は_clearRectangle_でタブごとスペース化される() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("a\tb\n");
        // col[0, 3) → 右境界 col 3 はタブ中央 → 広げて col 8 → "a\t" を削除
        // その後 実カラム幅 8 のスペース挿入 → "        b"
        var rect = new Rectangle(0, 0, 0, 3);
        RectangleGeometry.clearRectangle(window.getBuffer(), rect, 8);
        assertEquals("        b\n", window.getBuffer().getText());
    }

    @Test
    void 全角を跨ぐ矩形は_clearRectangle_で全角ごとスペース化される() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("aあb\n");
        // col[0, 2) → 右境界 col 2 はあの中央 → 広げて col 3 → "aあ" を削除
        // 実カラム幅 3 のスペース挿入 → "   b"
        var rect = new Rectangle(0, 0, 0, 2);
        RectangleGeometry.clearRectangle(window.getBuffer(), rect, 8);
        assertEquals("   b\n", window.getBuffer().getText());
    }

    @Test
    void 行が右カラムに届かない場合は実矩形幅のみスペースで埋める() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("a\nbb\n");
        window.setMark(0);
        window.setPoint(4); // 行1 col 2

        new ClearRectangleCommand().execute(context).join();

        // 行0 "a" は leftCp=0, rightCp=1 (行末)。削除して実幅 1 のスペースで埋める → " "
        // 行1 "bb" は leftCp=0, rightCp=2。削除して実幅 2 のスペースで埋める → "  "
        assertEquals(" \n  \n", window.getBuffer().getText());
    }
}
