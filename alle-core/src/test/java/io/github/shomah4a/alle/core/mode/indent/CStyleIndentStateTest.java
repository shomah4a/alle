package io.github.shomah4a.alle.core.mode.indent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CStyleIndentStateTest {

    private static final CStyleIndentConfig JS_CONFIG =
            new CStyleIndentConfig(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));

    private static Window createWindow(String text) {
        var buffer = new TextBuffer("test.js", new GapTextModel(), new SettingsRegistry());
        var facade = new BufferFacade(buffer);
        var window = new Window(facade);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        return window;
    }

    private static CStyleIndentState createState() {
        var registry = SyntaxAnalyzerRegistry.createWithBuiltins();
        var support = registry.create("javascript").orElseThrow();
        return new CStyleIndentState(JS_CONFIG, support.analyzer());
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
        void 前行のインデントが継承される() {
            var window = createWindow("  var x = 1;");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("  var x = 1;\n  ", bufferText(window));
        }

        @Test
        void 開き波括弧の後にインデントが増加する() {
            var window = createWindow("function f() {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("function f() {\n  ", bufferText(window));
        }

        @Test
        void 開き丸括弧の後にインデントが増加する() {
            var window = createWindow("foo(");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("foo(\n  ", bufferText(window));
        }

        @Test
        void 開き角括弧の後にインデントが増加する() {
            var window = createWindow("const arr = [");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("const arr = [\n  ", bufferText(window));
        }

        @Test
        void インデント済み行の開き括弧でさらにインデントが増加する() {
            var window = createWindow("  if (true) {");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("  if (true) {\n    ", bufferText(window));
        }

        @Test
        void コメント付き行末の開き括弧でインデントが増加する() {
            var window = createWindow("function f() { // comment\n}");
            // カーソルを1行目末尾（改行直前）に移動
            window.setPoint("function f() { // comment".length());
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("function f() { // comment\n  \n}", bufferText(window));
        }

        @Test
        void 括弧を含まない行では前行インデントを維持する() {
            var window = createWindow("    var x = 1;");
            var state = createState();
            state.newlineAndIndent(window);
            assertEquals("    var x = 1;\n    ", bufferText(window));
        }
    }

    @Nested
    class cycleIndent {

        @Test
        void 前行と同じインデントレベルにサイクルする() {
            var window = createWindow("  var x = 1;\nvar y = 2;");
            var state = createState();
            state.cycleIndent(window, 1);
            assertEquals("  var x = 1;\n  var y = 2;", bufferText(window));
        }

        @Test
        void 連続サイクルでインデントレベルが循環する() {
            var window = createWindow("    var x = 1;\nvar y = 2;");
            var state = createState();
            // 候補: [4, 2, 0]
            state.cycleIndent(window, 1); // 0 → 4
            assertEquals("    var x = 1;\n    var y = 2;", bufferText(window));
            state.cycleIndent(window, 1); // 4 → 2
            assertEquals("    var x = 1;\n  var y = 2;", bufferText(window));
            state.cycleIndent(window, 1); // 2 → 0
            assertEquals("    var x = 1;\nvar y = 2;", bufferText(window));
        }

        @Test
        void 逆方向サイクルでインデントが減少する() {
            var window = createWindow("  var x = 1;\nvar y = 2;");
            var state = createState();
            state.cycleIndent(window, -1);
            assertEquals("  var x = 1;\n  var y = 2;", bufferText(window));
        }

        @Test
        void 開き括弧で終わる前行の後でTabを押すとインデントが増加する() {
            var window = createWindow("function f() {\n  // here\n}");
            int line1Start = "function f() {\n".length();
            window.setPoint(line1Start + 2);

            var state = createState();
            state.cycleIndent(window, 1);
            assertEquals("function f() {\n// here\n}", bufferText(window));
            state.cycleIndent(window, 1);
            assertEquals("function f() {\n  // here\n}", bufferText(window));
        }

        @Test
        void 開き括弧で終わる前行の後でインデント0からTabで括弧内インデントになる() {
            var window = createWindow("function f() {\n// here\n}");
            int line1Start = "function f() {\n".length();
            window.setPoint(line1Start);

            var state = createState();
            state.cycleIndent(window, 1);
            assertEquals("function f() {\n  // here\n}", bufferText(window));
        }
    }
}
