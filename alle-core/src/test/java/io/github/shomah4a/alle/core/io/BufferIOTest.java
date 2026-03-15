package io.github.shomah4a.alle.core.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
            var io = new BufferIO(inMemoryReader(), inMemoryWriter());
            var result = io.load(Path.of("/tmp/test.txt"));

            assertEquals("test.txt", result.buffer().getName());
            assertEquals("Hello\nWorld", result.buffer().getText());
            assertEquals(LineEnding.LF, result.lineEnding());
            assertEquals(LineEnding.LF, result.buffer().getLineEnding());
            assertFalse(result.buffer().isDirty());
            assertEquals(Path.of("/tmp/test.txt"), result.buffer().getFilePath().orElseThrow());
        }

        @Test
        void CRLFのファイルを読み込むとLFに正規化される() throws IOException {
            storage.put("/tmp/crlf.txt", "Hello\r\nWorld\r\n");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter());
            var result = io.load(Path.of("/tmp/crlf.txt"));

            assertEquals("Hello\nWorld\n", result.buffer().getText());
            assertEquals(LineEnding.CRLF, result.lineEnding());
            assertEquals(LineEnding.CRLF, result.buffer().getLineEnding());
        }

        @Test
        void 空ファイルを読み込める() throws IOException {
            storage.put("/tmp/empty.txt", "");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter());
            var result = io.load(Path.of("/tmp/empty.txt"));

            assertEquals("", result.buffer().getText());
            assertEquals(0, result.buffer().length());
        }

        @Test
        void 絵文字を含むファイルを読み込める() throws IOException {
            storage.put("/tmp/emoji.txt", "Hello 😀\nWorld 🌍");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter());
            var result = io.load(Path.of("/tmp/emoji.txt"));

            assertEquals("Hello 😀\nWorld 🌍", result.buffer().getText());
        }
    }

    @Nested
    class save {

        @Test
        void バッファの内容をファイルに保存する() throws IOException {
            storage.put("/tmp/test.txt", "Hello\nWorld");
            var io = new BufferIO(inMemoryReader(), inMemoryWriter());
            var result = io.load(Path.of("/tmp/test.txt"));
            var buffer = result.buffer();

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
            var io = new BufferIO(inMemoryReader(), inMemoryWriter());
            var result = io.load(Path.of("/tmp/test.txt"));

            io.save(result.buffer());

            assertEquals(
                    "Hello\r\nWorld",
                    Objects.requireNonNull(writerStorage.get("/tmp/test.txt")).toString());
        }

        @Test
        void ファイルパスが未設定のバッファを保存すると例外が発生する() {
            var io = new BufferIO(inMemoryReader(), inMemoryWriter());
            var textModel = new io.github.shomah4a.alle.core.textmodel.GapTextModel();
            var buffer = new io.github.shomah4a.alle.core.buffer.Buffer("nopath", textModel);

            assertThrows(IllegalStateException.class, () -> io.save(buffer));
        }
    }
}
