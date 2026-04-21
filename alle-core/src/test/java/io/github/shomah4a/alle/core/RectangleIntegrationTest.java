package io.github.shomah4a.alle.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.input.DirectoryEntry;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

/**
 * EditorCore.create 経由の実機に近い構成で矩形コマンドを動かし、
 * undo 単位が期待通りであることを検証する。
 */
class RectangleIntegrationTest {

    private static final InputSource EMPTY_INPUT = Optional::empty;

    private EditorCore createCore(InputPrompter prompter) {
        var settings = new SettingsRegistry();
        settings.register(EditorSettings.INDENT_WIDTH);
        settings.register(EditorSettings.COMMENT_STRING);
        settings.register(EditorSettings.TAB_WIDTH);
        var bufferIO = new BufferIO(
                source -> new BufferedReader(new StringReader("")),
                destination -> new BufferedWriter(new StringWriter()),
                settings);

        return EditorCore.create(
                EMPTY_INPUT,
                frame -> prompter,
                bufferIO,
                dir -> Lists.immutable.<DirectoryEntry>empty(),
                () -> {},
                settings,
                Path.of("/tmp"));
    }

    private static void typeText(io.github.shomah4a.alle.core.command.CommandLoop loop, String text) {
        for (int i = 0; i < text.length(); i++) {
            loop.processKey(KeyStroke.of(text.charAt(i)));
        }
    }

    @Test
    void killRectangleを_CxRk_経由で実行すると1undoで戻る() {
        var core = createCore((msg, hist) -> CompletableFuture.completedFuture(new PromptResult.Cancelled()));

        var window = core.frame().getActiveWindow();
        var buffer = window.getBuffer();

        var loop = core.commandLoop();
        typeText(loop, "foo\nbar\nbaz\n");

        assertEquals("foo\nbar\nbaz\n", buffer.getText());

        // mark を先頭、point を z の後ろに移動
        window.setMark(0);
        window.setPoint(10); // 行2 col 2 = 'z' 上

        int sizeBefore = buffer.getUndoManager().undoSize();

        // C-x r k
        loop.processKey(KeyStroke.ctrl('x'));
        loop.processKey(KeyStroke.of('r'));
        loop.processKey(KeyStroke.of('k'));

        int sizeAfter = buffer.getUndoManager().undoSize();

        assertEquals("o\nr\nz\n", buffer.getText());
        assertEquals(
                sizeBefore + 1,
                sizeAfter,
                "kill-rectangle は 1 undo にまとまる必要がある (実際の増分: " + (sizeAfter - sizeBefore) + ")");
    }

    @Test
    void killRectangleを_Mx経由で実行すると1undoで戻る() {
        var core = createCore(
                (msg, hist) -> CompletableFuture.completedFuture(new PromptResult.Confirmed("kill-rectangle")));

        var window = core.frame().getActiveWindow();
        var buffer = window.getBuffer();

        var loop = core.commandLoop();
        typeText(loop, "foo\nbar\nbaz\n");

        assertEquals("foo\nbar\nbaz\n", buffer.getText());

        window.setMark(0);
        window.setPoint(10);

        int sizeBefore = buffer.getUndoManager().undoSize();

        // M-x → "kill-rectangle" (prompter が即座に返す)
        loop.processKey(KeyStroke.meta('x'));

        int sizeAfter = buffer.getUndoManager().undoSize();

        assertEquals("o\nr\nz\n", buffer.getText());
        assertEquals(
                sizeBefore + 1,
                sizeAfter,
                "M-x kill-rectangle は 1 undo にまとまる必要がある (実際の増分: " + (sizeAfter - sizeBefore) + ")");
    }
}
