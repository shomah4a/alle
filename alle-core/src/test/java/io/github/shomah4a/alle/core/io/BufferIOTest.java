package io.github.shomah4a.alle.core.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Objects;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferIOTest {

    private final MutableMap<String, String> storage = Maps.mutable.empty();

    private BufferReader inMemoryReader() {
        return source -> new StringReader(storage.getOrDefault(source, ""));
    }

    private final MutableMap<String, StringWriter> writerStorage = Maps.mutable.empty();

    private BufferWriter inMemoryWriter() {
        return destination -> {
            var writer = new StringWriter();
            writerStorage.put(destination, writer);
            return writer;
        };
    }

    @Nested
    class load {

        @Test
        void ファイルを読み込んでバッファを生成する() throws IOException {
            storage.put("/tmp/test.txt", "Hello\nWorld");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter(), new SettingsRegistry());
            var result = io.load(Path.of("/tmp/test.txt"));

            assertEquals("test.txt", result.bufferFacade().getName());
            assertEquals("Hello\nWorld", result.bufferFacade().getText());
            assertEquals(LineEnding.LF, result.lineEnding());
            assertEquals(LineEnding.LF, result.bufferFacade().getLineEnding());
            assertFalse(result.bufferFacade().isDirty());
            assertEquals(
                    Path.of("/tmp/test.txt"),
                    result.bufferFacade().getFilePath().orElseThrow());
        }

        @Test
        void CRLFのファイルを読み込むとLFに正規化される() throws IOException {
            storage.put("/tmp/crlf.txt", "Hello\r\nWorld\r\n");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter(), new SettingsRegistry());
            var result = io.load(Path.of("/tmp/crlf.txt"));

            assertEquals("Hello\nWorld\n", result.bufferFacade().getText());
            assertEquals(LineEnding.CRLF, result.lineEnding());
            assertEquals(LineEnding.CRLF, result.bufferFacade().getLineEnding());
        }

        @Test
        void 空ファイルを読み込める() throws IOException {
            storage.put("/tmp/empty.txt", "");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter(), new SettingsRegistry());
            var result = io.load(Path.of("/tmp/empty.txt"));

            assertEquals("", result.bufferFacade().getText());
            assertEquals(0, result.bufferFacade().length());
        }

        @Test
        void 絵文字を含むファイルを読み込める() throws IOException {
            storage.put("/tmp/emoji.txt", "Hello 😀\nWorld 🌍");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter(), new SettingsRegistry());
            var result = io.load(Path.of("/tmp/emoji.txt"));

            assertEquals("Hello 😀\nWorld 🌍", result.bufferFacade().getText());
        }
    }

    @Nested
    class save {

        @Test
        void バッファの内容をファイルに保存する() throws IOException {
            storage.put("/tmp/test.txt", "Hello\nWorld");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter(), new SettingsRegistry());
            var result = io.load(Path.of("/tmp/test.txt"));
            var buffer = result.bufferFacade();

            buffer.insertText(buffer.length(), "\nFoo");

            io.save(buffer);

            assertEquals(
                    "Hello\nWorld\nFoo",
                    Objects.requireNonNull(writerStorage.get("/tmp/test.txt")).toString());
            assertFalse(buffer.isDirty());
        }

        @Test
        void CRLF形式で保存できる() throws IOException {
            storage.put("/tmp/test.txt", "Hello\r\nWorld");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter(), new SettingsRegistry());
            var result = io.load(Path.of("/tmp/test.txt"));

            io.save(result.bufferFacade());

            assertEquals(
                    "Hello\r\nWorld",
                    Objects.requireNonNull(writerStorage.get("/tmp/test.txt")).toString());
        }

        @Test
        void ファイルパスが未設定のバッファを保存すると例外が発生する() {
            var io = new BufferIO(inMemoryReader(), inMemoryWriter(), new SettingsRegistry());
            var textModel = new io.github.shomah4a.alle.core.textmodel.GapTextModel();
            var buffer = new io.github.shomah4a.alle.core.buffer.BufferFacade(
                    new io.github.shomah4a.alle.core.buffer.TextBuffer("nopath", textModel, new SettingsRegistry()));

            assertThrows(IllegalStateException.class, () -> io.save(buffer));
        }
    }
}
