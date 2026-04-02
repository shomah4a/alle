package io.github.shomah4a.alle.core.mode.indent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.eclipse.collections.api.factory.Sets;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CStyleIndentStateTest {

    private static final CStyleIndentConfig JS_CONFIG =
            CStyleIndentConfig.of(2, Sets.immutable.with('(', '[', '{'), Sets.immutable.with(')', ']', '}'));

    private static Window createWindow(String text) {
        var buffer = new TextBuffer("test", new GapTextModel(), new SettingsRegistry());
        var facade = new BufferFacade(buffer);
        var window = new Window(facade);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        return window;
    }

    private static String bufferText(Window window) {
        return window.getBuffer().getText();
    }

    @Nested
    class newlineAndIndent {

        @Test
        void 空行で改行するとインデントなしで改行される() {
            var window = createWindow("");
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.newlineAndIndent(window);
            assertEquals("\n", bufferText(window));
        }

        @Test
        void 前行のインデントが継承される() {
            var window = createWindow("  hello");
            // カーソルは末尾にある
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.newlineAndIndent(window);
            assertEquals("  hello\n  ", bufferText(window));
        }

        @Test
        void 開き波括弧の後にインデントが増加する() {
            var window = createWindow("function() {");
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.newlineAndIndent(window);
            assertEquals("function() {\n  ", bufferText(window));
        }

        @Test
        void 開き丸括弧の後にインデントが増加する() {
            var window = createWindow("foo(");
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.newlineAndIndent(window);
            assertEquals("foo(\n  ", bufferText(window));
        }

        @Test
        void 開き角括弧の後にインデントが増加する() {
            var window = createWindow("const arr = [");
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.newlineAndIndent(window);
            assertEquals("const arr = [\n  ", bufferText(window));
        }

        @Test
        void インデント済み行の開き括弧でさらにインデントが増加する() {
            var window = createWindow("  if (true) {");
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.newlineAndIndent(window);
            assertEquals("  if (true) {\n    ", bufferText(window));
        }

        @Test
        void コメント付き行末の開き括弧でインデントが増加する() {
            var window = createWindow("{ // comment");
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.newlineAndIndent(window);
            assertEquals("{ // comment\n  ", bufferText(window));
        }

        @Test
        void 括弧を含まない行では前行インデントを維持する() {
            var window = createWindow("    x = 1;");
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.newlineAndIndent(window);
            assertEquals("    x = 1;\n    ", bufferText(window));
        }
    }

    @Nested
    class cycleIndent {

        @Test
        void 前行と同じインデントレベルにサイクルする() {
            var window = createWindow("  foo\nbar");
            // カーソルを2行目末尾に置く（insertで既に末尾にいる）
            var state = new CStyleIndentState(JS_CONFIG, null);
            state.cycleIndent(window, 1);
            // 前行インデント2がある → 候補は [2, 0] → 0からサイクルで2
            assertEquals("  foo\n  bar", bufferText(window));
        }

        @Test
        void 連続サイクルでインデントレベルが循環する() {
            var window = createWindow("    foo\nbar");
            var state = new CStyleIndentState(JS_CONFIG, null);
            // 候補: [4, 2, 0]
            state.cycleIndent(window, 1); // 0 → 4
            assertEquals("    foo\n    bar", bufferText(window));
            state.cycleIndent(window, 1); // 4 → 2
            assertEquals("    foo\n  bar", bufferText(window));
            state.cycleIndent(window, 1); // 2 → 0
            assertEquals("    foo\nbar", bufferText(window));
        }

        @Test
        void 逆方向サイクルでインデントが減少する() {
            var window = createWindow("  foo\nbar");
            var state = new CStyleIndentState(JS_CONFIG, null);
            // 候補: [2, 0] → 0はindex 1、逆方向で 0
            state.cycleIndent(window, -1); // 0 → index -1 mod 2 = 1 → 0のまま？
            // 現在 0 → indexは1 → (1 + -1) % 2 = 0 → candidates[0] = 2
            assertEquals("  foo\n  bar", bufferText(window));
        }

        @Test
        void 開き括弧で終わる前行の後でTabを押すとインデントが増加する() {
            // function () {
            //   // here   ← カーソルはここ
            // }
            var window = createWindow("function () {\n  // here\n}");
            // カーソルを2行目に移動（行1の先頭+2=インデント後）
            int line1Start = "function () {\n".length();
            window.setPoint(line1Start + 2);

            var state = new CStyleIndentState(JS_CONFIG, null);
            // 前行(行0)はインデント0、開き括弧で終わる→候補に0+2=2が含まれるべき
            state.cycleIndent(window, 1);
            // 現在2→次の候補へ。候補に2と0が含まれ、2→0になるはず
            assertEquals("function () {\n// here\n}", bufferText(window));
            state.cycleIndent(window, 1);
            // 0→2に戻る
            assertEquals("function () {\n  // here\n}", bufferText(window));
        }

        @Test
        void 開き括弧で終わる前行の後でインデント0からTabで括弧内インデントになる() {
            var window = createWindow("function () {\n// here\n}");
            int line1Start = "function () {\n".length();
            window.setPoint(line1Start);

            var state = new CStyleIndentState(JS_CONFIG, null);
            // 現在インデント0、前行インデント0で開き括弧→候補に2が含まれるべき
            state.cycleIndent(window, 1);
            assertEquals("function () {\n  // here\n}", bufferText(window));
        }
    }
}
