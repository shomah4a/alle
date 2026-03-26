package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TextChangeTest {

    private Buffer createBuffer(String initialText) {
        var buffer = new TextBuffer("test", new GapTextModel(), new SettingsRegistry());
        if (!initialText.isEmpty()) {
            buffer.insertText(0, initialText);
        }
        return buffer;
    }

    @Nested
    class 逆操作 {

        @Test
        void Insertの逆操作はDeleteになる() {
            var insert = new TextChange.Insert(0, "Hello");
            var inverse = insert.inverse();

            var delete = assertInstanceOf(TextChange.Delete.class, inverse);
            assertEquals(0, delete.offset());
            assertEquals("Hello", delete.text());
        }

        @Test
        void Deleteの逆操作はInsertになる() {
            var delete = new TextChange.Delete(3, "abc");
            var inverse = delete.inverse();

            var insert = assertInstanceOf(TextChange.Insert.class, inverse);
            assertEquals(3, insert.offset());
            assertEquals("abc", insert.text());
        }

        @Test
        void 逆操作の逆操作は元の操作と等しい() {
            var original = new TextChange.Insert(5, "World");
            assertEquals(original, original.inverse().inverse());
        }
    }

    @Nested
    class Bufferの操作が逆操作を返す {

        @Test
        void insertTextがDelete逆操作を返す() {
            var buffer = createBuffer("");
            var change = buffer.insertText(0, "Hello");

            var delete = assertInstanceOf(TextChange.Delete.class, change);
            assertEquals(0, delete.offset());
            assertEquals("Hello", delete.text());
            assertEquals("Hello", buffer.getText());
        }

        @Test
        void deleteTextがInsert逆操作を返す() {
            var buffer = createBuffer("Hello World");
            var change = buffer.deleteText(5, 6);

            var insert = assertInstanceOf(TextChange.Insert.class, change);
            assertEquals(5, insert.offset());
            assertEquals(" World", insert.text());
            assertEquals("Hello", buffer.getText());
        }
    }

    @Nested
    class applyによる適用 {

        @Test
        void Insert変更を適用するとテキストが挿入される() {
            var buffer = createBuffer("Hello");
            var change = new TextChange.Insert(5, " World");

            var inverse = buffer.apply(change);

            assertEquals("Hello World", buffer.getText());
            assertInstanceOf(TextChange.Delete.class, inverse);
        }

        @Test
        void Delete変更を適用するとテキストが削除される() {
            var buffer = createBuffer("Hello World");
            var change = new TextChange.Delete(5, " World");

            var inverse = buffer.apply(change);

            assertEquals("Hello", buffer.getText());
            assertInstanceOf(TextChange.Insert.class, inverse);
        }

        @Test
        void 逆操作を適用すると元に戻る() {
            var buffer = createBuffer("Hello");
            var change = buffer.insertText(5, " World");
            assertEquals("Hello World", buffer.getText());

            buffer.apply(change);
            assertEquals("Hello", buffer.getText());
        }

        @Test
        void undo後にredoで元に戻る() {
            var buffer = createBuffer("Hello");
            var change = buffer.insertText(5, " World");
            assertEquals("Hello World", buffer.getText());

            var redo = buffer.apply(change);
            assertEquals("Hello", buffer.getText());

            buffer.apply(redo);
            assertEquals("Hello World", buffer.getText());
        }

        @Test
        void 絵文字を含むテキストのundo_redoが正しく動作する() {
            var buffer = createBuffer("A");
            var change = buffer.insertText(1, "😀B");
            assertEquals("A😀B", buffer.getText());

            buffer.apply(change);
            assertEquals("A", buffer.getText());
        }
    }
}
