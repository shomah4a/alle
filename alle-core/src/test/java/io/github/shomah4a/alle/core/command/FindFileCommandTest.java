package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.io.BufferReader;
import io.github.shomah4a.alle.core.io.BufferWriter;
import io.github.shomah4a.alle.core.io.LineEnding;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.TextMode;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FindFileCommandTest {

    private final MutableMap<String, String> storage = Maps.mutable.empty();
    private final DirectoryLister stubLister = directory -> Lists.immutable.empty();
    private final AutoModeMap autoModeMap = new AutoModeMap(TextMode::new);
    private Frame frame;
    private BufferManager bufferManager;
    private BufferIO bufferIO;

    @BeforeEach
    void setUp() {
        var buffer = new EditableBuffer("*scratch*", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);

        BufferReader reader = source -> {
            if (!storage.containsKey(source)) {
                throw new IOException("File not found: " + source);
            }
            return new StringReader(storage.get(source));
        };
        BufferWriter writer = destination -> new StringWriter();
        bufferIO = new BufferIO(reader, writer);
    }

    private InputPrompter confirming(String value) {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private InputPrompter cancelling() {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
    }

    @Nested
    class ファイルを開く {

        @Test
        void 指定パスのファイルを読み込みバッファに表示する() {
            storage.put("/tmp/hello.txt", "Hello\nWorld");
            var cmd = new FindFileCommand(bufferIO, stubLister, Path.of("/test"), autoModeMap, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("/tmp/hello.txt"));

            cmd.execute(context).join();

            assertEquals("hello.txt", frame.getActiveWindow().getBuffer().getName());
            assertEquals("Hello\nWorld", frame.getActiveWindow().getBuffer().getText());
            assertEquals(
                    Path.of("/tmp/hello.txt"),
                    frame.getActiveWindow().getBuffer().getFilePath().orElseThrow());
            assertEquals(0, frame.getActiveWindow().getPoint());
        }

        @Test
        void 読み込んだバッファがBufferManagerに追加される() {
            storage.put("/tmp/hello.txt", "Hello");
            var cmd = new FindFileCommand(bufferIO, stubLister, Path.of("/test"), autoModeMap, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("/tmp/hello.txt"));

            cmd.execute(context).join();

            assertEquals(2, bufferManager.size());
            assertTrue(bufferManager.findByName("hello.txt").isPresent());
        }

        @Test
        void CRLFファイルのLineEndingがバッファに保持される() {
            storage.put("/tmp/crlf.txt", "Hello\r\nWorld");
            var cmd = new FindFileCommand(bufferIO, stubLister, Path.of("/test"), autoModeMap, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("/tmp/crlf.txt"));

            cmd.execute(context).join();

            assertEquals(LineEnding.CRLF, frame.getActiveWindow().getBuffer().getLineEnding());
        }
    }

    @Nested
    class ファイルが存在しない場合 {

        @Test
        void 空バッファがファイルパス付きで作成される() {
            var cmd = new FindFileCommand(bufferIO, stubLister, Path.of("/test"), autoModeMap, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("/tmp/new.txt"));

            cmd.execute(context).join();

            assertEquals("new.txt", frame.getActiveWindow().getBuffer().getName());
            assertEquals("", frame.getActiveWindow().getBuffer().getText());
            assertEquals(
                    Path.of("/tmp/new.txt"),
                    frame.getActiveWindow().getBuffer().getFilePath().orElseThrow());
        }
    }

    @Nested
    class 同一パスのバッファが既に存在する場合 {

        @Test
        void 既存バッファに切り替わる() {
            storage.put("/tmp/hello.txt", "Hello");
            var cmd = new FindFileCommand(bufferIO, stubLister, Path.of("/test"), autoModeMap, new InputHistory());

            // 1回目: ファイルを開く
            var context1 = TestCommandContextFactory.create(frame, bufferManager, confirming("/tmp/hello.txt"));
            cmd.execute(context1).join();

            // バッファにテキスト追加
            frame.getActiveWindow().getBuffer().insertText(5, "!");

            // 2回目: 同じファイルを開く
            var context2 = TestCommandContextFactory.create(frame, bufferManager, confirming("/tmp/hello.txt"));
            cmd.execute(context2).join();

            // 新しいバッファが作られず、既存のバッファ（編集済み）が使われる
            assertEquals(2, bufferManager.size());
            assertEquals("Hello!", frame.getActiveWindow().getBuffer().getText());
        }
    }

    @Nested
    class パス正規化 {

        @Test
        void 相対パスが絶対パスに変換される() {
            var normalized = FindFileCommand.normalizePath("hello.txt");

            assertTrue(normalized.isAbsolute());
        }

        @Test
        void 親ディレクトリ参照が解決される() {
            var normalized = FindFileCommand.normalizePath("/tmp/foo/../bar.txt");

            assertEquals(Path.of("/tmp/bar.txt"), normalized);
        }

        @Test
        void 冗長なスラッシュが除去される() {
            var normalized = FindFileCommand.normalizePath("/tmp//hello.txt");

            assertEquals(Path.of("/tmp/hello.txt"), normalized);
        }
    }

    @Nested
    class キャンセル {

        @Test
        void キャンセル時は何も変わらない() {
            var cmd = new FindFileCommand(bufferIO, stubLister, Path.of("/test"), autoModeMap, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, cancelling());

            cmd.execute(context).join();

            assertEquals("*scratch*", frame.getActiveWindow().getBuffer().getName());
            assertEquals(1, bufferManager.size());
        }
    }

    @Nested
    class 空文字列入力 {

        @Test
        void 空文字列で確定した場合は何も変わらない() {
            var cmd = new FindFileCommand(bufferIO, stubLister, Path.of("/test"), autoModeMap, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming(""));

            cmd.execute(context).join();

            assertEquals("*scratch*", frame.getActiveWindow().getBuffer().getName());
            assertEquals(1, bufferManager.size());
        }
    }
}
