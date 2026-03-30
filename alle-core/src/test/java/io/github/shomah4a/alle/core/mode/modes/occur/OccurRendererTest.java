package io.github.shomah4a.alle.core.mode.modes.occur;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OccurRendererTest {

    private static BufferFacade createBuffer(String name, String text) {
        var settings = new SettingsRegistry();
        var buffer = new TextBuffer(name, new GapTextModel(), settings);
        var facade = new BufferFacade(buffer);
        if (!text.isEmpty()) {
            facade.insertText(0, text);
        }
        return facade;
    }

    @Nested
    class buildText {

        @Test
        void ヘッダ行にマッチ件数とクエリとバッファ名が表示される() {
            var source = createBuffer("test.txt", "foo\nbar\nfoo bar");
            var model = OccurModel.search(source, "foo");
            String text = OccurRenderer.buildText(model);

            String[] lines = text.split("\n", -1);
            assertEquals("2 lines matching \"foo\" in test.txt", lines[0]);
        }

        @Test
        void マッチ行に行番号とテキストが表示される() {
            var source = createBuffer("test.txt", "aaa\nbbb\nccc\nbbb again");
            var model = OccurModel.search(source, "bbb");
            String text = OccurRenderer.buildText(model);

            String[] lines = text.split("\n", -1);
            assertEquals(3, lines.length);
            assertEquals("    2: bbb", lines[1]);
            assertEquals("    4: bbb again", lines[2]);
        }

        @Test
        void 行番号が右寄せで揃う() {
            // 100行以上あるバッファで3桁の行番号
            var sb = new StringBuilder();
            for (int i = 0; i < 105; i++) {
                if (i > 0) {
                    sb.append('\n');
                }
                sb.append(i == 2 ? "match" : i == 104 ? "match" : "no");
            }
            var source = createBuffer("test.txt", sb.toString());
            var model = OccurModel.search(source, "match");
            String text = OccurRenderer.buildText(model);

            String[] lines = text.split("\n", -1);
            assertEquals("      3: match", lines[1]);
            assertEquals("    105: match", lines[2]);
        }

        @Test
        void マッチなしの場合はヘッダ行のみ() {
            var source = createBuffer("test.txt", "foo\nbar");
            var model = OccurModel.search(source, "xyz");
            String text = OccurRenderer.buildText(model);

            assertEquals("0 lines matching \"xyz\" in test.txt", text);
        }
    }
}
