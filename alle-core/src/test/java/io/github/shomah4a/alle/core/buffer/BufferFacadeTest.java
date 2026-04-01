package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferFacadeTest {

    private TextBuffer editableBuffer;
    private BufferFacade facade;

    @BeforeEach
    void setUp() {
        editableBuffer = new TextBuffer("test", new GapTextModel(), new SettingsRegistry());
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
            messageBuffer = new MessageBuffer("*Messages*", 100, new SettingsRegistry());
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
            assertThrows(ReadOnlyBufferException.class, () -> readOnlyFacade.setFilePath(Path.of("/tmp/test")));
        }

        @Test
        void isReadOnlyがtrueを返す() {
            assertTrue(readOnlyFacade.isReadOnly());
        }
    }

    @Nested
    class setReadOnlyフラグ {

        @Test
        void setReadOnlyでバッファを読み取り専用に設定できる() {
            assertFalse(facade.isReadOnly());

            facade.setReadOnly(true);

            assertTrue(facade.isReadOnly());
        }

        @Test
        void 読み取り専用に設定するとinsertTextがブロックされる() {
            facade.setReadOnly(true);

            assertThrows(ReadOnlyBufferException.class, () -> facade.insertText(0, "x"));
        }

        @Test
        void 読み取り専用に設定するとdeleteTextがブロックされる() {
            editableBuffer.insertText(0, "hello");
            facade.setReadOnly(true);

            assertThrows(ReadOnlyBufferException.class, () -> facade.deleteText(0, 1));
        }

        @Test
        void setReadOnlyをfalseにすると編集が再び可能になる() {
            facade.setReadOnly(true);
            facade.setReadOnly(false);

            facade.insertText(0, "hello");
            assertEquals("hello", facade.getText());
        }
    }

    @Nested
    class デフォルトディレクトリ {

        @Test
        void ファイルパスが設定されていればその親ディレクトリを返す() {
            editableBuffer.setFilePath(Path.of("/home/user/project/src/Main.java"));

            assertEquals(
                    Path.of("/home/user/project/src"), facade.getDefaultDirectory(Path.of("/fallback"), p -> false));
        }

        @Test
        void ファイルパスが未設定の場合はフォールバックを返す() {
            assertEquals(Path.of("/fallback"), facade.getDefaultDirectory(Path.of("/fallback"), p -> false));
        }

        @Test
        void ルートパス直下のファイルの場合はルートディレクトリを返す() {
            editableBuffer.setFilePath(Path.of("/file.txt"));

            assertEquals(Path.of("/"), facade.getDefaultDirectory(Path.of("/fallback"), p -> false));
        }

        @Test
        void ディレクトリパスが設定されていればそのディレクトリ自身を返す() {
            editableBuffer.setFilePath(Path.of("/home/user/project"));

            assertEquals(Path.of("/home/user/project"), facade.getDefaultDirectory(Path.of("/fallback"), p -> true));
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
            var otherBuffer = new TextBuffer("other", new GapTextModel(), new SettingsRegistry());
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
