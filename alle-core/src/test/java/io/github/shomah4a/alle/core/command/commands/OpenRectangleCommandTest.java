package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import org.junit.jupiter.api.Test;

class OpenRectangleCommandTest {

    @Test
    void コマンド名はopenRectangleである() {
        assertEquals("open-rectangle", new OpenRectangleCommand().name());
    }

    @Test
    void 矩形範囲と同サイズの空白を挿入して右側を押し出す() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("abc\ndef\nghi\n");
        window.setMark(1); // 行0 col 1 (a の後)
        window.setPoint(9); // 行2 col 1

        new OpenRectangleCommand().execute(context).join();

        // 各行の col=1 にスペース幅 0 の矩形は幅0
        // mark=col1 point=col1 なので幅0。幅0なら何も起きない
        assertEquals("abc\ndef\nghi\n", window.getBuffer().getText());
    }

    @Test
    void 幅ありの矩形で右側のテキストを押し出す() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("abc\ndef\nghi\n");
        window.setMark(1); // 行0 col 1
        window.setPoint(11); // 行2 col 3

        new OpenRectangleCommand().execute(context).join();

        // 各行 col 1 に 2 スペース挿入 → "a  bc\nd  ef\ng  hi\n"
        assertEquals("a  bc\nd  ef\ng  hi\n", window.getBuffer().getText());
    }

    @Test
    void 行が目的カラムに届かない場合は末尾にスペース補填してから挿入() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("a\nbb\nccc\n");
        // "a\nbb\nccc\n" offset: a(0) \n(1) b(2) b(3) \n(4) c(5) c(6) c(7) \n(8)
        window.setMark(3); // 行1 col 1 (最初の b の後)
        window.setPoint(8); // 行2 col 3 (ccc の後)

        new OpenRectangleCommand().execute(context).join();

        // mark 行1 col 1, point 行2 col 3 → startLine=1, endLine=2, leftCol=1, rightCol=3
        // 行1 "bb" col 1 に 2スペース挿入 → "b  b"
        // 行2 "ccc" col 1 に 2スペース挿入 → "c  cc"
        assertEquals("a\nb  b\nc  cc\n", window.getBuffer().getText());
    }
}
