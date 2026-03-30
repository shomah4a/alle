package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UndoRedoCommandTest {

    @Nested
    class Undo {

        @Test
        void コマンド名はundoである() {
            assertEquals("undo", new UndoCommand().name());
        }

        @Test
        void 挿入操作を取り消す() {
            var context = TestCommandContextFactory.createDefault();
            var window = context.frame().getActiveWindow();
            window.insert("Hello");
            assertEquals("Hello", window.getBuffer().getText());

            new UndoCommand().execute(context).join();

            assertEquals("", window.getBuffer().getText());
            assertEquals(0, window.getPoint());
        }

        @Test
        void 削除操作を取り消す() {
            var context = TestCommandContextFactory.createDefault();
            var window = context.frame().getActiveWindow();
            window.insert("Hello");
            window.setPoint(4);
            window.deleteForward(1);
            assertEquals("Hell", window.getBuffer().getText());

            new UndoCommand().execute(context).join();

            assertEquals("Hello", window.getBuffer().getText());
            assertEquals(4, window.getPoint());
        }

        @Test
        void 複数回undoする() {
            var context = TestCommandContextFactory.createDefault();
            var window = context.frame().getActiveWindow();
            window.insert("A");
            window.insert("B");
            assertEquals("AB", window.getBuffer().getText());

            new UndoCommand().execute(context).join();
            assertEquals("A", window.getBuffer().getText());

            new UndoCommand().execute(context).join();
            assertEquals("", window.getBuffer().getText());
        }

        @Test
        void undo履歴がない場合は何もしない() {
            var context = TestCommandContextFactory.createDefault();
            var window = context.frame().getActiveWindow();

            new UndoCommand().execute(context).join();

            assertEquals("", window.getBuffer().getText());
            assertEquals(0, window.getPoint());
        }

        @Test
        void undo操作自体はundo履歴に記録されない() {
            var context = TestCommandContextFactory.createDefault();
            var window = context.frame().getActiveWindow();
            window.insert("Hello");

            new UndoCommand().execute(context).join();
            assertEquals("", window.getBuffer().getText());

            // 再度undoしても何も起きない（undo自体は記録されていない）
            new UndoCommand().execute(context).join();
            assertEquals("", window.getBuffer().getText());
        }
    }

    @Nested
    class Redo {

        @Test
        void コマンド名はredoである() {
            assertEquals("redo", new RedoCommand().name());
        }

        @Test
        void undoした操作をやり直す() {
            var context = TestCommandContextFactory.createDefault();
            var window = context.frame().getActiveWindow();
            window.insert("Hello");
            new UndoCommand().execute(context).join();
            assertEquals("", window.getBuffer().getText());

            new RedoCommand().execute(context).join();

            assertEquals("Hello", window.getBuffer().getText());
            assertEquals(5, window.getPoint());
        }

        @Test
        void redo履歴がない場合は何もしない() {
            var context = TestCommandContextFactory.createDefault();
            var window = context.frame().getActiveWindow();
            window.insert("Hello");

            new RedoCommand().execute(context).join();

            assertEquals("Hello", window.getBuffer().getText());
        }

        @Test
        void 通常の編集後はredoできない() {
            var context = TestCommandContextFactory.createDefault();
            var window = context.frame().getActiveWindow();
            window.insert("Hello");
            new UndoCommand().execute(context).join();
            window.insert("World");

            new RedoCommand().execute(context).join();

            // redoスタックがクリアされているのでredoは何もしない
            assertEquals("World", window.getBuffer().getText());
        }
    }
}
