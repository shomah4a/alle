package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.input.DirectoryEntry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.io.BufferReader;
import io.github.shomah4a.alle.core.io.BufferWriter;
import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SaveBufferCommandTest {

    private final DirectoryLister stubLister = directory -> Lists.immutable.<DirectoryEntry>empty();
    private Frame frame;
    private BufferManager bufferManager;
    private final MutableMap<String, StringWriter> writerStorage = Maps.mutable.empty();
    private BufferIO bufferIO;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);

        BufferReader reader = source -> new StringReader("");
        BufferWriter writer = destination -> {
            var sw = new StringWriter();
            writerStorage.put(destination, sw);
            return sw;
        };
        bufferIO = new BufferIO(reader, writer, new SettingsRegistry());
    }

    private InputPrompter confirming(String value) {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private InputPrompter cancelling() {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
    }

    @Nested
    class ファイルパスが設定済みの場合 {

        @Test
        void バッファの内容がファイルに保存される() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.setFilePath(Path.of("/tmp/test.txt"));
            buffer.insertText(0, "Hello\nWorld");
            buffer.markDirty();

            var cmd = new SaveBufferCommand(bufferIO, stubLister, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals(
                    "Hello\nWorld",
                    Objects.requireNonNull(writerStorage.get("/tmp/test.txt")).toString());
        }

        @Test
        void 保存後にミニバッファにファイルパスを含むメッセージが表示される() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.setFilePath(Path.of("/tmp/test.txt"));
            buffer.insertText(0, "Hello");

            var cmd = new SaveBufferCommand(bufferIO, stubLister, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals(
                    "Saved: /tmp/test.txt",
                    context.messageBuffer().getLastMessage().orElseThrow());
        }

        @Test
        void 保存後にダーティフラグがクリアされる() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.setFilePath(Path.of("/tmp/test.txt"));
            buffer.insertText(0, "Hello");
            buffer.markDirty();
            assertTrue(buffer.isDirty());

            var cmd = new SaveBufferCommand(bufferIO, stubLister, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertFalse(buffer.isDirty());
        }

        @Test
        void CRLFのLineEndingで保存される() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.setFilePath(Path.of("/tmp/crlf.txt"));
            buffer.setLineEnding(LineEnding.CRLF);
            buffer.insertText(0, "Hello\nWorld");

            var cmd = new SaveBufferCommand(bufferIO, stubLister, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals(
                    "Hello\r\nWorld",
                    Objects.requireNonNull(writerStorage.get("/tmp/crlf.txt")).toString());
        }
    }

    @Nested
    class ファイルパスが未設定の場合 {

        @Test
        void プロンプトで入力されたパスに保存される() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.insertText(0, "New file content");

            var cmd = new SaveBufferCommand(bufferIO, stubLister, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("/tmp/new.txt"));

            cmd.execute(context).join();

            assertEquals(
                    "New file content",
                    Objects.requireNonNull(writerStorage.get("/tmp/new.txt")).toString());
            assertEquals(Path.of("/tmp/new.txt"), buffer.getFilePath().orElseThrow());
        }

        @Test
        void プロンプト入力後の保存でミニバッファにメッセージが表示される() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.insertText(0, "New file content");

            var cmd = new SaveBufferCommand(bufferIO, stubLister, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("/tmp/new.txt"));

            cmd.execute(context).join();

            assertEquals(
                    "Saved: /tmp/new.txt",
                    context.messageBuffer().getLastMessage().orElseThrow());
        }

        @Test
        void プロンプトでキャンセルすると保存されない() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.insertText(0, "Content");

            var cmd = new SaveBufferCommand(bufferIO, stubLister, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, cancelling());

            cmd.execute(context).join();

            assertTrue(writerStorage.isEmpty());
            assertTrue(buffer.getFilePath().isEmpty());
        }

        @Test
        void プロンプトで空文字列を入力すると保存されない() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.insertText(0, "Content");

            var cmd = new SaveBufferCommand(bufferIO, stubLister, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming(""));

            cmd.execute(context).join();

            assertTrue(writerStorage.isEmpty());
            assertTrue(buffer.getFilePath().isEmpty());
        }
    }
}
