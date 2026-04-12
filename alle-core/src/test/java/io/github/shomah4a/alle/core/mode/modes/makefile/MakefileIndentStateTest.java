package io.github.shomah4a.alle.core.mode.modes.makefile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MakefileIndentStateTest {

    private static Window createWindow(String text, boolean useTabs) {
        var buffer = new TextBuffer("Makefile", new GapTextModel(), new SettingsRegistry());
        var facade = new BufferFacade(buffer);
        facade.getSettings().setLocal(EditorSettings.INDENT_TABS_MODE, useTabs);
        var window = new Window(facade);
        if (!text.isEmpty()) {
            window.insert(text);
        }
        return window;
    }

    private static Window createTabWindow(String text) {
        return createWindow(text, true);
    }

    private static Window createSpaceWindow(String text) {
        return createWindow(text, false);
    }

    private static String bufferText(Window window) {
        return window.getBuffer().getText();
    }

    @Nested
    class ターゲット行判定 {

        @Test
        void シンプルなターゲット行を検出する() {
            assertTrue(MakefileIndentState.isTargetLine("all: main.o"));
        }

        @Test
        void 依存関係なしのターゲット行を検出する() {
            assertTrue(MakefileIndentState.isTargetLine("clean:"));
        }

        @Test
        void 変数代入行はターゲットと判定しない() {
            assertFalse(MakefileIndentState.isTargetLine("CC = gcc"));
        }

        @Test
        void コロンイコールの変数代入はターゲットと判定しない() {
            assertFalse(MakefileIndentState.isTargetLine("CC := gcc"));
        }

        @Test
        void クエスチョンイコールの変数代入はターゲットと判定しない() {
            assertFalse(MakefileIndentState.isTargetLine("CC ?= gcc"));
        }

        @Test
        void プラスイコールの変数代入はターゲットと判定しない() {
            assertFalse(MakefileIndentState.isTargetLine("CFLAGS += -Wall"));
        }

        @Test
        void コメント行はターゲットと判定しない() {
            assertFalse(MakefileIndentState.isTargetLine("# comment: here"));
        }

        @Test
        void タブで始まるレシピ行はターゲットと判定しない() {
            assertFalse(MakefileIndentState.isTargetLine("\tgcc -o main main.o"));
        }
    }

    @Nested
    class レシピ行判定 {

        @Test
        void タブで始まる行をレシピ行と判定する() {
            assertTrue(MakefileIndentState.isRecipeLine("\tgcc -o main main.o"));
        }

        @Test
        void スペースで始まる行はレシピ行と判定しない() {
            assertFalse(MakefileIndentState.isRecipeLine("    gcc -o main main.o"));
        }

        @Test
        void インデントなしの行はレシピ行と判定しない() {
            assertFalse(MakefileIndentState.isRecipeLine("all: main.o"));
        }
    }

    @Nested
    class タブモードでのnewlineAndIndent {

        @Test
        void ターゲット行の後にタブ付きで改行される() {
            var window = createTabWindow("all: main.o");
            var state = new MakefileIndentState();
            state.newlineAndIndent(window);
            assertEquals("all: main.o\n\t", bufferText(window));
        }

        @Test
        void 依存関係なしターゲットの後にタブ付きで改行される() {
            var window = createTabWindow("clean:");
            var state = new MakefileIndentState();
            state.newlineAndIndent(window);
            assertEquals("clean:\n\t", bufferText(window));
        }

        @Test
        void レシピ行の後にタブ付きで改行される() {
            var window = createTabWindow("all: main.o\n\tgcc -o main main.o");
            var state = new MakefileIndentState();
            state.newlineAndIndent(window);
            assertEquals("all: main.o\n\tgcc -o main main.o\n\t", bufferText(window));
        }

        @Test
        void 変数代入行の後はインデントなしで改行される() {
            var window = createTabWindow("CC = gcc");
            var state = new MakefileIndentState();
            state.newlineAndIndent(window);
            assertEquals("CC = gcc\n", bufferText(window));
        }

        @Test
        void 空行の後はインデントなしで改行される() {
            var window = createTabWindow("");
            var state = new MakefileIndentState();
            state.newlineAndIndent(window);
            assertEquals("\n", bufferText(window));
        }

        @Test
        void コメント行の後はインデントなしで改行される() {
            var window = createTabWindow("# comment");
            var state = new MakefileIndentState();
            state.newlineAndIndent(window);
            assertEquals("# comment\n", bufferText(window));
        }
    }

    @Nested
    class スペースモードでのnewlineAndIndent {

        @Test
        void ターゲット行の後にスペースインデント付きで改行される() {
            var window = createSpaceWindow("all: main.o");
            var state = new MakefileIndentState();
            state.newlineAndIndent(window);
            assertEquals("all: main.o\n    ", bufferText(window));
        }

        @Test
        void 変数代入行の後はインデントなしで改行される() {
            var window = createSpaceWindow("CC = gcc");
            var state = new MakefileIndentState();
            state.newlineAndIndent(window);
            assertEquals("CC = gcc\n", bufferText(window));
        }
    }

    @Nested
    class タブモードでのcycleIndent {

        @Test
        void インデントなしの行でTabを押すとタブが挿入される() {
            var window = createTabWindow("echo hello");
            window.setPoint(0);
            var state = new MakefileIndentState();
            state.cycleIndent(window, 1);
            assertEquals("\techo hello", bufferText(window));
        }

        @Test
        void タブ付きの行でTabを押すとインデントが除去される() {
            var window = createTabWindow("\techo hello");
            window.setPoint(0);
            var state = new MakefileIndentState();
            state.cycleIndent(window, 1);
            assertEquals("echo hello", bufferText(window));
        }

        @Test
        void ShiftTabで逆方向にサイクルする() {
            var window = createTabWindow("echo hello");
            window.setPoint(0);
            var state = new MakefileIndentState();
            state.cycleIndent(window, -1);
            assertEquals("\techo hello", bufferText(window));
        }

        @Test
        void 連続サイクルでインデントが循環する() {
            var window = createTabWindow("echo hello");
            window.setPoint(0);
            var state = new MakefileIndentState();

            state.cycleIndent(window, 1);
            assertEquals("\techo hello", bufferText(window));

            state.cycleIndent(window, 1);
            assertEquals("echo hello", bufferText(window));
        }
    }

    @Nested
    class スペースモードでのcycleIndent {

        @Test
        void インデントなしの行でTabを押すとスペースインデントが挿入される() {
            var window = createSpaceWindow("echo hello");
            window.setPoint(0);
            var state = new MakefileIndentState();
            state.cycleIndent(window, 1);
            assertEquals("    echo hello", bufferText(window));
        }

        @Test
        void スペースインデント付きの行でTabを押すとインデントが除去される() {
            var window = createSpaceWindow("    echo hello");
            window.setPoint(0);
            var state = new MakefileIndentState();
            state.cycleIndent(window, 1);
            assertEquals("echo hello", bufferText(window));
        }
    }
}
