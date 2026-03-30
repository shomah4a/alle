package io.github.shomah4a.alle.core.mode.modes.occur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OccurEntryResolverTest {

    private static BufferFacade createSourceBuffer(String text) {
        var settings = new SettingsRegistry();
        var buffer = new TextBuffer("source.txt", new GapTextModel(), settings);
        var facade = new BufferFacade(buffer);
        if (!text.isEmpty()) {
            facade.insertText(0, text);
        }
        return facade;
    }

    private static Window createOccurWindow(OccurModel model) {
        var settings = new SettingsRegistry();
        var occurBuffer = new TextBuffer("*Occur source.txt*", new GapTextModel(), settings);
        var facade = new BufferFacade(occurBuffer);
        var mode = new OccurMode(model, new Keymap("occur"), new CommandRegistry());
        facade.setMajorMode(mode);
        OccurRenderer.render(facade, model);
        facade.setReadOnly(true);
        return new Window(facade);
    }

    @Nested
    class resolve {

        @Test
        void ヘッダ行ではemptyを返す() {
            var source = createSourceBuffer("foo\nbar\nfoo bar");
            var model = OccurModel.search(source, "foo");
            var window = createOccurWindow(model);
            window.setPoint(0);

            var result = OccurEntryResolver.resolve(
                    window, (OccurMode) window.getBuffer().getMajorMode());
            assertTrue(result.isEmpty());
        }

        @Test
        void マッチ行1行目で対応するOccurMatchが返る() {
            var source = createSourceBuffer("foo\nbar\nfoo bar");
            var model = OccurModel.search(source, "foo");
            var window = createOccurWindow(model);

            // 2行目（最初のマッチ行）にポイントを移動
            int lineStart = window.getBuffer().lineStartOffset(1);
            window.setPoint(lineStart);

            var result = OccurEntryResolver.resolve(
                    window, (OccurMode) window.getBuffer().getMajorMode());
            assertTrue(result.isPresent());
            assertEquals(0, result.get().lineIndex());
            assertEquals("foo", result.get().lineText());
        }

        @Test
        void マッチ行2行目で対応するOccurMatchが返る() {
            var source = createSourceBuffer("foo\nbar\nfoo bar");
            var model = OccurModel.search(source, "foo");
            var window = createOccurWindow(model);

            // 3行目（2番目のマッチ行）にポイントを移動
            int lineStart = window.getBuffer().lineStartOffset(2);
            window.setPoint(lineStart);

            var result = OccurEntryResolver.resolve(
                    window, (OccurMode) window.getBuffer().getMajorMode());
            assertTrue(result.isPresent());
            assertEquals(2, result.get().lineIndex());
            assertEquals("foo bar", result.get().lineText());
        }
    }
}
