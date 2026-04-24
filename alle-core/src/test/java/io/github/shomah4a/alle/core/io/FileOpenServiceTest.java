package io.github.shomah4a.alle.core.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FileOpenServiceTest {

    private final MutableMap<String, String> storage = Maps.mutable.empty();
    private final AutoModeMap autoModeMap = new AutoModeMap(TextMode::new);
    private final SettingsRegistry settingsRegistry = new SettingsRegistry();
    private Frame frame;
    private BufferManager bufferManager;
    private FileOpenService service;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), settingsRegistry));
        var window = new Window(buffer);
        var minibuffer =
                new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settingsRegistry)));
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
        var bufferIO = new BufferIO(reader, writer, settingsRegistry);
        service = new FileOpenService(bufferIO, autoModeMap, new ModeRegistry(), settingsRegistry);
    }

    @Nested
    class ファイルを開く {

        @Test
        void 指定パスのファイルを読み込みバッファに表示する() {
            storage.put("/tmp/hello.txt", "Hello\nWorld");

            service.openFile("/tmp/hello.txt", bufferManager, frame);

            assertEquals("hello.txt", frame.getActiveWindow().getBuffer().getName());
            assertEquals("Hello\nWorld", frame.getActiveWindow().getBuffer().getText());
            assertEquals(
                    Path.of("/tmp/hello.txt"),
                    frame.getActiveWindow().getBuffer().getFilePath().orElseThrow());
        }

        @Test
        void 読み込んだバッファがBufferManagerに追加される() {
            storage.put("/tmp/hello.txt", "Hello");

            service.openFile("/tmp/hello.txt", bufferManager, frame);

            assertEquals(2, bufferManager.size());
            assertTrue(bufferManager.findByName("hello.txt").isPresent());
        }

        @Test
        void CRLFファイルのLineEndingがバッファに保持される() {
            storage.put("/tmp/crlf.txt", "Hello\r\nWorld");

            service.openFile("/tmp/crlf.txt", bufferManager, frame);

            assertEquals(LineEnding.CRLF, frame.getActiveWindow().getBuffer().getLineEnding());
        }

        @Test
        void メジャーモードが自動設定される() {
            storage.put("/tmp/test.txt", "hello");

            service.openFile("/tmp/test.txt", bufferManager, frame);

            assertEquals(
                    "text", frame.getActiveWindow().getBuffer().getMajorMode().name());
        }
    }

    @Nested
    class ファイルが存在しない場合 {

        @Test
        void 空バッファがファイルパス付きで作成される() {
            service.openFile("/tmp/new.txt", bufferManager, frame);

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

            service.openFile("/tmp/hello.txt", bufferManager, frame);

            // バッファにテキスト追加
            frame.getActiveWindow().getBuffer().insertText(5, "!");

            // 同じファイルを開く
            service.openFile("/tmp/hello.txt", bufferManager, frame);

            // 新しいバッファが作られず、既存のバッファ（編集済み）が使われる
            assertEquals(2, bufferManager.size());
            assertEquals("Hello!", frame.getActiveWindow().getBuffer().getText());
        }
    }

    @Nested
    class 空文字列パス {

        @Test
        void 何も変わらない() {
            var beforeName = frame.getActiveWindow().getBuffer().getName();

            service.openFile("", bufferManager, frame);

            assertEquals(beforeName, frame.getActiveWindow().getBuffer().getName());
            assertEquals(1, bufferManager.size());
        }
    }

    @Nested
    class 末尾スラッシュ {

        @Test
        void 末尾スラッシュが除去されて正規化される() {
            storage.put("/tmp/hello.txt", "Hello");

            service.openFile("/tmp/hello.txt/", bufferManager, frame);

            assertEquals("hello.txt", frame.getActiveWindow().getBuffer().getName());
        }
    }

    @Nested
    class パス正規化 {

        @Test
        void 相対パスが絶対パスに変換される() {
            var normalized = FileOpenService.normalizePath("hello.txt");

            assertTrue(normalized.isAbsolute());
        }

        @Test
        void 親ディレクトリ参照が解決される() {
            var normalized = FileOpenService.normalizePath("/tmp/foo/../bar.txt");

            assertEquals(Path.of("/tmp/bar.txt"), normalized);
        }

        @Test
        void 冗長なスラッシュが除去される() {
            var normalized = FileOpenService.normalizePath("/tmp//hello.txt");

            assertEquals(Path.of("/tmp/hello.txt"), normalized);
        }
    }
}
