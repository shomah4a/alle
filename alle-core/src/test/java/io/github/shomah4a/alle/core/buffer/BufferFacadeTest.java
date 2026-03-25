package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferFacadeTest {

    private EditableBuffer editableBuffer;
    private BufferFacade facade;

    @BeforeEach
    void setUp() {
        editableBuffer = new EditableBuffer("test", new GapTextModel());
        facade = new BufferFacade(editableBuffer);
    }

    @Nested
    class 読み取り委譲 {

        @Test
        void テキストの読み取りがラップ先に委譲される() {
            editableBuffer.insertText(0, "hello");

            assertEquals("hello", facade.getText());
            assertEquals(5, facade.length());
            assertEquals('h', facade.codePointAt(0));
            assertEquals("ell", facade.substring(1, 4));
        }

        @Test
        void 行情報がラップ先に委譲される() {
            editableBuffer.insertText(0, "line1\nline2");

            assertEquals(2, facade.lineCount());
            assertEquals("line1", facade.lineText(0));
            assertEquals("line2", facade.lineText(1));
            assertEquals(0, facade.lineIndexForOffset(0));
            assertEquals(1, facade.lineIndexForOffset(6));
            assertEquals(0, facade.lineStartOffset(0));
            assertEquals(6, facade.lineStartOffset(1));
        }

        @Test
        void メタデータがラップ先に委譲される() {
            assertEquals("test", facade.getName());
            assertFalse(facade.isDirty());
            assertFalse(facade.isReadOnly());
        }
    }

    @Nested
    class 書き込み可能バッファ {

        @Test
        void isReadOnlyがfalseの場合は書き込みが成功する() {
            facade.insertText(0, "abc");

            assertEquals("abc", editableBuffer.getText());
        }

        @Test
        void deleteTextが成功する() {
            editableBuffer.insertText(0, "abc");

            facade.deleteText(1, 1);

            assertEquals("ac", editableBuffer.getText());
        }
    }

    @Nested
    class 読み取り専用バッファ {

        private MessageBuffer messageBuffer;
        private BufferFacade readOnlyFacade;

        @BeforeEach
        void setUp() {
            messageBuffer = new MessageBuffer("*Messages*", 100);
            readOnlyFacade = new BufferFacade(messageBuffer);
        }

        @Test
        void insertTextでReadOnlyBufferExceptionがスローされる() {
            assertThrows(ReadOnlyBufferException.class, () -> readOnlyFacade.insertText(0, "x"));
        }

        @Test
        void deleteTextでReadOnlyBufferExceptionがスローされる() {
            assertThrows(ReadOnlyBufferException.class, () -> readOnlyFacade.deleteText(0, 1));
        }

        @Test
        void markDirtyでReadOnlyBufferExceptionがスローされる() {
            assertThrows(ReadOnlyBufferException.class, () -> readOnlyFacade.markDirty());
        }

        @Test
        void setFilePathでReadOnlyBufferExceptionがスローされる() {
            assertThrows(
                    ReadOnlyBufferException.class,
                    () -> readOnlyFacade.setFilePath(java.nio.file.Path.of("/tmp/test")));
        }

        @Test
        void isReadOnlyがtrueを返す() {
            assertTrue(readOnlyFacade.isReadOnly());
        }
    }

    @Nested
    class 同一性 {

        @Test
        void 同じバッファをラップしたFacade同士はequalsがtrueを返す() {
            var facade2 = new BufferFacade(editableBuffer);

            assertEquals(facade, facade2);
        }

        @Test
        void 異なるバッファをラップしたFacadeはequalsがfalseを返す() {
            var otherBuffer = new EditableBuffer("other", new GapTextModel());
            var otherFacade = new BufferFacade(otherBuffer);

            assertFalse(facade.equals(otherFacade));
        }

        @Test
        void 同じバッファをラップしたFacade同士はhashCodeが一致する() {
            var facade2 = new BufferFacade(editableBuffer);

            assertEquals(facade.hashCode(), facade2.hashCode());
        }

        @Test
        void FacadeとラップされたバッファのhashCodeが一致する() {
            assertEquals(facade.hashCode(), editableBuffer.hashCode());
        }
    }
}
