package io.github.shomah4a.allei.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.allei.core.textmodel.GapTextModel;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferTest {

    private Buffer createBuffer() {
        return new Buffer("test", new GapTextModel());
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

            var bufferWithPath = new Buffer("test", new GapTextModel(), Path.of("/tmp/test.txt"));
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
}
