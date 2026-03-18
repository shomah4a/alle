package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferTest {

    private Buffer createBuffer() {
        return new EditableBuffer("test", new GapTextModel());
    }

    @Nested
    class 初期状態 {

        @Test
        void 名前とテキストモデルを保持する() {
            var buffer = createBuffer();
            assertEquals("test", buffer.getName());
            assertEquals(0, buffer.length());
        }

        @Test
        void 初期状態はクリーン() {
            var buffer = createBuffer();
            assertFalse(buffer.isDirty());
        }

        @Test
        void ファイルパスはオプショナル() {
            var buffer = createBuffer();
            assertTrue(buffer.getFilePath().isEmpty());

            var bufferWithPath = new EditableBuffer("test", new GapTextModel(), Path.of("/tmp/test.txt"));
            assertEquals(Path.of("/tmp/test.txt"), bufferWithPath.getFilePath().orElseThrow());
        }
    }

    @Nested
    class ダーティフラグ {

        @Test
        void markDirtyでダーティになる() {
            var buffer = createBuffer();
            assertFalse(buffer.isDirty());
            buffer.markDirty();
            assertTrue(buffer.isDirty());
        }

        @Test
        void markCleanでクリーンに戻る() {
            var buffer = createBuffer();
            buffer.markDirty();
            assertTrue(buffer.isDirty());
            buffer.markClean();
            assertFalse(buffer.isDirty());
        }
    }

    @Nested
    class テキスト取得 {

        @Test
        void テキストモデルの内容を取得できる() {
            var buffer = createBuffer();
            buffer.insertText(0, "Hello");
            assertEquals("Hello", buffer.getText());
        }
    }

    @Nested
    class 部分readOnly {

        @Test
        void readOnly領域への挿入でReadOnlyBufferExceptionがスローされる() {
            var buffer = createBuffer();
            buffer.insertText(0, "prompt: input");
            buffer.putReadOnly(0, 8);

            assertThrows(ReadOnlyBufferException.class, () -> buffer.insertText(3, "x"));
        }

        @Test
        void readOnly領域への削除でReadOnlyBufferExceptionがスローされる() {
            var buffer = createBuffer();
            buffer.insertText(0, "prompt: input");
            buffer.putReadOnly(0, 8);

            assertThrows(ReadOnlyBufferException.class, () -> buffer.deleteText(0, 3));
        }

        @Test
        void readOnly領域の直後への挿入は許可される() {
            var buffer = createBuffer();
            buffer.insertText(0, "prompt: ");
            buffer.putReadOnly(0, 8);

            buffer.insertText(8, "hello");
            assertEquals("prompt: hello", buffer.getText());
        }

        @Test
        void readOnly領域外への挿入は許可される() {
            var buffer = createBuffer();
            buffer.insertText(0, "prompt: input");
            buffer.putReadOnly(0, 8);

            buffer.insertText(13, "!");
            assertEquals("prompt: input!", buffer.getText());
        }

        @Test
        void readOnly領域外の削除は許可される() {
            var buffer = createBuffer();
            buffer.insertText(0, "prompt: input");
            buffer.putReadOnly(0, 8);

            buffer.deleteText(8, 5);
            assertEquals("prompt: ", buffer.getText());
        }

        @Test
        void removeReadOnly後は編集可能になる() {
            var buffer = createBuffer();
            buffer.insertText(0, "prompt: input");
            buffer.putReadOnly(0, 8);

            buffer.removeReadOnly(0, 8);
            buffer.insertText(0, "x");
            assertEquals("xprompt: input", buffer.getText());
        }

        @Test
        void readOnly領域の直後への挿入後もreadOnly範囲は拡大しない() {
            var buffer = createBuffer();
            buffer.insertText(0, "ab");
            buffer.putReadOnly(0, 2);

            // rear-nonsticky: 位置2への挿入はread-only外
            buffer.insertText(2, "cd");
            assertEquals("abcd", buffer.getText());

            // 挿入後も位置2はread-onlyではない
            assertFalse(buffer.isReadOnlyAt(2));
            assertFalse(buffer.isReadOnlyAt(3));
        }

        @Test
        void readOnly領域外の削除後にreadOnly範囲が維持される() {
            var buffer = createBuffer();
            buffer.insertText(0, "prompt: hello world");
            buffer.putReadOnly(0, 8);

            buffer.deleteText(8, 6);
            assertEquals("prompt: world", buffer.getText());

            // read-only範囲は[0, 8)のまま
            assertTrue(buffer.isReadOnlyAt(0));
            assertTrue(buffer.isReadOnlyAt(7));
            assertFalse(buffer.isReadOnlyAt(8));
        }
    }
}
