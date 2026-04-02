package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferHistoryEntryTest {

    @Nested
    class ファクトリメソッド {

        @Test
        void ファイルパスを持つバッファからByPathが生成される() {
            var buffer = new BufferFacade(new TextBuffer("file.txt", new GapTextModel(), new SettingsRegistry()));
            var filePath = Path.of("/tmp/file.txt");
            buffer.setFilePath(filePath);

            var entry = BufferHistoryEntry.of(buffer);

            var byPath = assertInstanceOf(BufferHistoryEntry.ByPath.class, entry);
            assertEquals(filePath, byPath.path());
        }

        @Test
        void ファイルパスを持たないバッファからByNameが生成される() {
            var buffer = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), new SettingsRegistry()));

            var entry = BufferHistoryEntry.of(buffer);

            var byName = assertInstanceOf(BufferHistoryEntry.ByName.class, entry);
            assertEquals("*scratch*", byName.name());
        }

        @Test
        void displayNameが設定されたファイルパス付きバッファからByPathが生成される() {
            var buffer = new BufferFacade(new TextBuffer("file.txt", new GapTextModel(), new SettingsRegistry()));
            var filePath = Path.of("/tmp/dir/file.txt");
            buffer.setFilePath(filePath);

            var entry = BufferHistoryEntry.of(buffer);

            var byPath = assertInstanceOf(BufferHistoryEntry.ByPath.class, entry);
            assertEquals(filePath, byPath.path());
        }
    }
}
