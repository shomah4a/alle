package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import org.junit.jupiter.api.Test;

class DeleteRectangleCommandTest {

    @Test
    void コマンド名はdeleteRectangleである() {
        assertEquals("delete-rectangle", new DeleteRectangleCommand().name());
    }

    @Test
    void 矩形範囲を削除する() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\nbaz\n");
        window.setMark(0);
        window.setPoint(10);

        new DeleteRectangleCommand().execute(context).join();

        // 各行の col[0, 2) を削除 → "o\nr\nz\n"
        assertEquals("o\nr\nz\n", window.getBuffer().getText());
    }

    @Test
    void markが未設定の場合は何もしない() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\n");

        new DeleteRectangleCommand().execute(context).join();

        assertEquals("foo\nbar\n", window.getBuffer().getText());
    }

    @Test
    void 幅0の矩形は何も削除しない() {
        var context = TestCommandContextFactory.createDefault();
        var window = context.frame().getActiveWindow();
        window.insert("foo\nbar\n");
        window.setMark(0);
        window.setPoint(4); // 行 1 の先頭

        new DeleteRectangleCommand().execute(context).join();

        assertEquals("foo\nbar\n", window.getBuffer().getText());
    }
}
