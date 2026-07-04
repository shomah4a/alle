package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonPrettyPrintCommandTest {

    private static final SettingsRegistry SETTINGS = createSettings();

    private Frame frame;
    private BufferFacade buffer;
    private BufferManager bufferManager;

    private static SettingsRegistry createSettings() {
        var registry = new SettingsRegistry();
        registry.register(EditorSettings.INDENT_WIDTH);
        registry.register(EditorSettings.INDENT_TABS_MODE);
        return registry;
    }

    @BeforeEach
    void setUp() {
        buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), SETTINGS));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
    }

    private CommandContext createContext() {
        return TestCommandContextFactory.create(frame, bufferManager);
    }

    private Window activeWindow() {
        return frame.getActiveWindow();
    }

    @Nested
    class WholeBuffer {

        @Test
        void オブジェクトが整形される() {
            buffer.insertText(0, "{\"a\":1,\"b\":2}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("{\n    \"a\": 1,\n    \"b\": 2\n}", buffer.getText());
        }

        @Test
        void 配列が整形される() {
            buffer.insertText(0, "[1,2,3]");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("[\n    1,\n    2,\n    3\n]", buffer.getText());
        }

        @Test
        void 整形後カーソルは先頭に移動する() {
            buffer.insertText(0, "{\"a\":1}");
            activeWindow().setPoint(5);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals(0, activeWindow().getPoint());
            assertTrue(activeWindow().getMark().isEmpty());
        }

        @Test
        void 末尾改行は保持される() {
            buffer.insertText(0, "{\"a\":1}\n");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertTrue(buffer.getText().endsWith("\n"));
        }

        @Test
        void 空バッファでは何もしない() {
            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("", buffer.getText());
        }

        @Test
        void 既に整形済みなら変更されない() {
            String pretty = "{\n    \"a\": 1\n}";
            buffer.insertText(0, pretty);
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals(pretty, buffer.getText());
        }

        @Test
        void 複数のJSON値が改行で連結される() {
            buffer.insertText(0, "{\"a\":1}\n{\"b\":2}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("{\n    \"a\": 1\n}\n{\n    \"b\": 2\n}", buffer.getText());
        }
    }

    @Nested
    class Region {

        @Test
        void リージョン内のJSONだけが整形される() {
            buffer.insertText(0, "prefix{\"a\":1}suffix");
            activeWindow().setMark(6);
            activeWindow().setPoint(13);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("prefix{\n    \"a\": 1\n}suffix", buffer.getText());
        }

        @Test
        void 整形後markは先頭でpointは末尾に設定される() {
            buffer.insertText(0, "prefix{\"a\":1}suffix");
            activeWindow().setMark(6);
            activeWindow().setPoint(13);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            int expectedStart = 6;
            int expectedEnd = expectedStart + "{\n    \"a\": 1\n}".length();
            assertEquals(expectedStart, activeWindow().getMark().orElseThrow());
            assertEquals(expectedEnd, activeWindow().getPoint());
        }

        @Test
        void 逆順のリージョンでも整形されmarkとpointが正順化される() {
            buffer.insertText(0, "prefix{\"a\":1}suffix");
            activeWindow().setMark(13);
            activeWindow().setPoint(6);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("prefix{\n    \"a\": 1\n}suffix", buffer.getText());
            int expectedStart = 6;
            int expectedEnd = expectedStart + "{\n    \"a\": 1\n}".length();
            assertEquals(expectedStart, activeWindow().getMark().orElseThrow());
            assertEquals(expectedEnd, activeWindow().getPoint());
        }

        @Test
        void 空リージョンはバッファ全体扱いになる() {
            buffer.insertText(0, "{\"a\":1}");
            activeWindow().setPoint(3);
            activeWindow().setMark(3);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("{\n    \"a\": 1\n}", buffer.getText());
        }

        @Test
        void リージョン内の複数のJSON値が改行で連結される() {
            buffer.insertText(0, "head{\"a\":1}{\"b\":2}tail");
            activeWindow().setMark(4);
            activeWindow().setPoint(18);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("head{\n    \"a\": 1\n}\n{\n    \"b\": 2\n}tail", buffer.getText());
        }

        @Test
        void 既に整形済みのリージョンではmarkとpointが保持される() {
            String pretty = "{\n    \"a\": 1\n}";
            buffer.insertText(0, "head" + pretty + "tail");
            int markPos = 4;
            int pointPos = markPos + pretty.length();
            activeWindow().setMark(markPos);
            activeWindow().setPoint(pointPos);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("head" + pretty + "tail", buffer.getText());
            assertEquals(markPos, activeWindow().getMark().orElseThrow());
            assertEquals(pointPos, activeWindow().getPoint());
        }

        @Test
        void リージョン整形時は前後の空白と改行が吸収される() {
            buffer.insertText(0, "head\n  {\"a\":1}  \ntail");
            activeWindow().setMark(4);
            activeWindow().setPoint(17);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("head{\n    \"a\": 1\n}tail", buffer.getText());
        }
    }

    @Nested
    class IndentSetting {

        @Test
        void INDENT_WIDTH_2でスペース2文字インデント() {
            buffer.getSettings().setLocal(EditorSettings.INDENT_WIDTH, 2);
            buffer.insertText(0, "{\"a\":1}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("{\n  \"a\": 1\n}", buffer.getText());
        }

        @Test
        void INDENT_TABS_MODE_trueでタブ文字インデント() {
            buffer.getSettings().setLocal(EditorSettings.INDENT_TABS_MODE, true);
            buffer.insertText(0, "{\"a\":1}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("{\n\t\"a\": 1\n}", buffer.getText());
        }
    }

    @Nested
    class ErrorHandling {

        @Test
        void 不正JSONではバッファが変更されずメッセージが表示される() {
            String original = "{invalid}";
            buffer.insertText(0, original);
            activeWindow().setPoint(0);
            var context = createContext();

            new JsonPrettyPrintCommand().execute(context).join();

            assertEquals(original, buffer.getText());
            assertTrue(context.messageBuffer().getLastMessage().orElse("").startsWith("JSON parse error:"));
        }

        @Test
        void ReadOnlyバッファでは変更されずメッセージにバッファ名が含まれる() {
            buffer.insertText(0, "{\"a\":1}");
            buffer.setReadOnly(true);
            activeWindow().setPoint(0);
            var context = createContext();

            new JsonPrettyPrintCommand().execute(context).join();

            assertEquals("{\"a\":1}", buffer.getText());
            String message = context.messageBuffer().getLastMessage().orElseThrow();
            assertTrue(message.startsWith("Buffer is read-only:"));
            assertTrue(message.contains("test"));
        }

        @Test
        void 空白のみの入力は変更されずメッセージが表示される() {
            buffer.insertText(0, "   \n  \n");
            activeWindow().setPoint(0);
            var context = createContext();

            new JsonPrettyPrintCommand().execute(context).join();

            assertEquals("   \n  \n", buffer.getText());
            assertEquals(
                    "No JSON value found",
                    context.messageBuffer().getLastMessage().orElseThrow());
        }

        @Test
        void 重複キーはパースエラーになりバッファは変更されない() {
            String original = "{\"a\":1,\"a\":2}";
            buffer.insertText(0, original);
            activeWindow().setPoint(0);
            var context = createContext();

            new JsonPrettyPrintCommand().execute(context).join();

            assertEquals(original, buffer.getText());
            assertTrue(context.messageBuffer().getLastMessage().orElse("").startsWith("JSON parse error:"));
        }
    }

    @Nested
    class NumberPrecision {

        @Test
        void 大きな整数が正確に保持される() {
            buffer.insertText(0, "{\"n\":123456789012345678901234567890}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertTrue(buffer.getText().contains("123456789012345678901234567890"));
        }

        @Test
        void 小数の後続ゼロが保持される() {
            buffer.insertText(0, "{\"n\":1.10}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertTrue(buffer.getText().contains("1.10"));
        }
    }

    @Nested
    class UnicodePreservation {

        @Test
        void 日本語が保持される() {
            buffer.insertText(0, "{\"key\":\"日本語\"}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertTrue(buffer.getText().contains("日本語"));
        }

        @Test
        void 絵文字_サロゲートペア_が保持される() {
            buffer.insertText(0, "{\"e\":\"😀\"}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertTrue(buffer.getText().contains("😀"));
        }
    }

    @Nested
    class EmptyStructures {

        @Test
        void 空オブジェクトはそのまま() {
            buffer.insertText(0, "{}");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("{ }", buffer.getText());
        }

        @Test
        void 空配列はそのまま() {
            buffer.insertText(0, "[]");
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertEquals("[ ]", buffer.getText());
        }
    }

    @Nested
    class Dirty {

        @Test
        void 整形後はダーティになる() {
            buffer.insertText(0, "{\"a\":1}");
            buffer.markClean();
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertTrue(buffer.isDirty());
        }

        @Test
        void 既に整形済みならダーティにならない() {
            buffer.insertText(0, "{\n    \"a\": 1\n}");
            buffer.markClean();
            activeWindow().setPoint(0);

            new JsonPrettyPrintCommand().execute(createContext()).join();

            assertFalse(buffer.isDirty());
        }
    }
}
