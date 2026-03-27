package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RevertBufferCommandTest {

    private final MutableMap<String, String> storage = Maps.mutable.empty();
    private Frame frame;
    private BufferManager bufferManager;
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

        bufferIO = new BufferIO(
                source -> new StringReader(storage.getOrDefault(source, "")),
                destination -> {
                    throw new UnsupportedOperationException();
                },
                new SettingsRegistry());
    }

    private InputPrompter confirming(String value) {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private void setupFileBuffer(String path, String content) {
        storage.put(path, content);
        var buffer = frame.getActiveWindow().getBuffer();
        buffer.setFilePath(Path.of(path));
        buffer.getUndoManager().withoutRecording(() -> {
            buffer.insertText(0, content);
        });
        buffer.markClean();
    }

    @Nested
    class ファイルパスが設定されていない場合 {

        @Test
        void メッセージを表示して中断する() {
            var cmd = new RevertBufferCommand(bufferIO);
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals(
                    "Buffer is not visiting a file",
                    context.messageBuffer().getLastMessage().orElseThrow());
        }
    }

    @Nested
    class readOnlyバッファの場合 {

        @Test
        void メッセージを表示して中断する() {
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.setFilePath(Path.of("/tmp/test.txt"));
            buffer.setReadOnly(true);

            var cmd = new RevertBufferCommand(bufferIO);
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals(
                    "Buffer is read-only: *scratch*",
                    context.messageBuffer().getLastMessage().orElseThrow());
        }
    }

    @Nested
    class バッファが未変更の場合 {

        @Test
        void 確認なしでrevertされる() {
            setupFileBuffer("/tmp/test.txt", "original");

            // ファイルの内容を更新
            storage.put("/tmp/test.txt", "updated");

            var cmd = new RevertBufferCommand(bufferIO);
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals("updated", frame.getActiveWindow().getBuffer().getText());
        }
    }

    @Nested
    class バッファが変更されている場合 {

        @Test
        void yesと回答するとrevertされる() {
            setupFileBuffer("/tmp/test.txt", "original");
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.insertText(buffer.length(), " modified");
            buffer.markDirty();

            storage.put("/tmp/test.txt", "reverted content");

            var cmd = new RevertBufferCommand(bufferIO);
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("yes"));

            cmd.execute(context).join();

            assertEquals("reverted content", buffer.getText());
            assertFalse(buffer.isDirty());
        }

        @Test
        void noと回答するとrevertされない() {
            setupFileBuffer("/tmp/test.txt", "original");
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.insertText(buffer.length(), " modified");
            buffer.markDirty();

            var cmd = new RevertBufferCommand(bufferIO);
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("no"));

            cmd.execute(context).join();

            assertEquals("original modified", buffer.getText());
        }

        @Test
        void キャンセルするとrevertされない() {
            setupFileBuffer("/tmp/test.txt", "original");
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.insertText(buffer.length(), " modified");
            buffer.markDirty();

            var cmd = new RevertBufferCommand(bufferIO);
            // NOOP_PROMPTERはCancelledを返す
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals("original modified", buffer.getText());
        }
    }

    @Nested
    class revert後の状態 {

        @Test
        void undo履歴がクリアされる() {
            setupFileBuffer("/tmp/test.txt", "original");
            var buffer = frame.getActiveWindow().getBuffer();
            buffer.insertText(buffer.length(), " edit");
            assertEquals(1, buffer.getUndoManager().undoSize());

            storage.put("/tmp/test.txt", "reloaded");

            var cmd = new RevertBufferCommand(bufferIO);
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals(0, buffer.getUndoManager().undoSize());
            assertEquals(0, buffer.getUndoManager().redoSize());
        }

        @Test
        void 成功メッセージが表示される() {
            setupFileBuffer("/tmp/test.txt", "original");
            storage.put("/tmp/test.txt", "updated");

            var cmd = new RevertBufferCommand(bufferIO);
            var context = TestCommandContextFactory.create(frame, bufferManager);

            cmd.execute(context).join();

            assertEquals(
                    "Reverted buffer from /tmp/test.txt",
                    context.messageBuffer().getLastMessage().orElseThrow());
        }
    }
}
