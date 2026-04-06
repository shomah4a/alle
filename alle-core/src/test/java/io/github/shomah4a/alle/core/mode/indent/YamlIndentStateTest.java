package io.github.shomah4a.alle.core.mode.indent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class YamlIndentStateTest {

    private static final int INDENT_WIDTH = 2;

    private static Window createWindow(String text) {
        var buffer = new TextBuffer("test.yml", new GapTextModel(), new SettingsRegistry());
        var facade = new BufferFacade(buffer);
        var window = new Window(facade);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        return window;
    }

    private static YamlIndentState createState() {
        var registry = SyntaxAnalyzerRegistry.createWithBuiltins();
        var support = registry.create("yaml").orElseThrow();
        return new YamlIndentState(INDENT_WIDTH, support.analyzer());
    }

    private static String bufferText(Window window) {
        return window.getBuffer().getText();
    }

    @Nested
    class newlineAndIndent {

        @Test
        void 空行で改行するとインデントなしで改行される() {
            var window = createWindow("");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("\n", bufferText(window));
        }

        @Test
        void 値なしのマッピングキー行で改行するとインデントが増加する() {
            var window = createWindow("server:");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("server:\n  ", bufferText(window));
        }

        @Test
        void ネストしたマッピングキー行で改行するとインデントが増加する() {
            var window = createWindow("server:\n  database:");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("server:\n  database:\n    ", bufferText(window));
        }

        @Test
        void 値ありのマッピング行で改行すると同じインデントが継承される() {
            var window = createWindow("server:\n  host: localhost");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("server:\n  host: localhost\n  ", bufferText(window));
        }

        @Test
        void トップレベルの値ありマッピング行で改行するとインデントなしで改行される() {
            var window = createWindow("key: value");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("key: value\n", bufferText(window));
        }

        @Test
        void フロースタイルマッピングの開き括弧の後で改行すると括弧内インデントになる() {
            // 閉じ括弧がないとflow_mappingノードが生成されないため、完全な形で入力する
            var window = createWindow("config: {key: val}");
            // カーソルを開き括弧の直後に移動
            window.setPoint("config: {".length());
            var state = createState();
            state.newlineAndIndent(window);
            // flow_mapping の開始カラム(8) + 1 = 9
            assertEquals("config: {\n         key: val}", bufferText(window));
        }

        @Test
        void フロースタイルシーケンスの開き括弧の後で改行すると括弧内インデントになる() {
            var window = createWindow("items: [1, 2]");
            // カーソルを開き括弧の直後に移動
            window.setPoint("items: [".length());
            var state = createState();
            state.newlineAndIndent(window);
            // flow_sequence の開始カラム(7) + 1 = 8
            assertEquals("items: [\n        1, 2]", bufferText(window));
        }
    }

    @Nested
    class cycleIndent {

        @Test
        void マッピングキー行の次行でTabを押すとインデントが増加する() {
            var window = createWindow("server:\nhost");
            var state = createState();
            state.cycleIndent(window, 1);
            assertEquals("server:\n  host", bufferText(window));
        }

        @Test
        void 連続サイクルでインデントレベルが循環する() {
            var window = createWindow("server:\n  database:\nname");
            var state = createState();
            // 前行(database:)はインデント2でキー行 → 候補に4が含まれるべき
            state.cycleIndent(window, 1);
            assertEquals("server:\n  database:\n    name", bufferText(window));
            state.cycleIndent(window, 1);
            assertEquals("server:\n  database:\n  name", bufferText(window));
            state.cycleIndent(window, 1);
            assertEquals("server:\n  database:\nname", bufferText(window));
        }

        @Test
        void 逆方向サイクルでインデントが減少する() {
            var window = createWindow("server:\n  host: localhost");
            // カーソルは末尾
            var state = createState();
            state.cycleIndent(window, -1);
            // 現在2 → 逆方向で候補を循環
            String result = bufferText(window);
            // インデントが変化すること
            assertEquals("server:\nhost: localhost", result);
        }
    }
}
