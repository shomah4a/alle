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
    void 行が右カラムに届かない場合は末尾にスペース補填() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("a\nbb\n");
        window.setMark(0);
        // "a\nbb\n" offset 4 は行1 col 2 (bb の後、\n の位置)
        window.setPoint(4);

        new ClearRectangleCommand().execute(context).join();

        // 行0 "a" [0, 2) → 幅2スペースで埋める → "  "
        // 行1 "bb" [0, 2) → "  "
        assertEquals("  \n  \n", window.getBuffer().getText());
    }
}
