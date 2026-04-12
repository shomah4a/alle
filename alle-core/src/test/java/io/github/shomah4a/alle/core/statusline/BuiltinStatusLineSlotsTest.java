package io.github.shomah4a.alle.core.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BuiltinStatusLineSlotsTest {

    private StatusLineRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new StatusLineRegistry();
        BuiltinStatusLineSlots.registerAll(registry);
    }

    private StatusLineContext createContext(String bufferName, String text) {
        var settingsRegistry = new SettingsRegistry();
        var model = new GapTextModel();
        model.insert(0, text);
        var buffer = new BufferFacade(new TextBuffer(bufferName, model, settingsRegistry));
        var window = new Window(buffer);
        return new StatusLineContext(window, buffer);
    }

    @Test
    void 標準スロットが全て登録される() {
        var names = registry.registeredNames();
        assertTrue(names.contains("buffer-status"));
        assertTrue(names.contains("truncate-indicator"));
        assertTrue(names.contains("buffer-name"));
        assertTrue(names.contains("line-number"));
        assertTrue(names.contains("column-number"));
        assertTrue(names.contains("major-mode"));
        assertTrue(names.contains("minor-modes"));
    }

    @Test
    void 標準グループが全て登録される() {
        var names = registry.registeredNames();
        assertTrue(names.contains("buffer-info"));
        assertTrue(names.contains("position"));
        assertTrue(names.contains("mode-info"));
        assertTrue(names.contains("misc-info"));
    }

    @Nested
    class 個別スロット {

        @Test
        void buffer_status_未変更バッファはハイフン2つを返す() {
            var ctx = createContext("test.txt", "");
            var slot = registry.lookup("buffer-status").orElseThrow();
            assertEquals("--", slot.render(ctx));
        }

        @Test
        void buffer_status_変更済みバッファはアスタリスク2つを返す() {
            var ctx = createContext("test.txt", "");
            ctx.buffer().insertText(0, "x");
            ctx.buffer().markDirty();
            assertEquals("**", registry.lookup("buffer-status").orElseThrow().render(ctx));
        }

        @Test
        void truncate_indicator_折り返しモードではバックスラッシュを返す() {
            var ctx = createContext("test.txt", "");
            assertEquals(
                    "\\", registry.lookup("truncate-indicator").orElseThrow().render(ctx));
        }

        @Test
        void truncate_indicator_切り詰めモードではドルを返す() {
            var ctx = createContext("test.txt", "");
            ctx.window().setTruncateLines(true);
            assertEquals(
                    "$", registry.lookup("truncate-indicator").orElseThrow().render(ctx));
        }

        @Test
        void buffer_name_バッファ名を返す() {
            var ctx = createContext("hello.py", "");
            assertEquals(
                    "hello.py", registry.lookup("buffer-name").orElseThrow().render(ctx));
        }

        @Test
        void line_number_カーソル位置の行番号を1始まりで返す() {
            var ctx = createContext("test.txt", "line1\nline2\nline3");
            // カーソルを2行目の先頭に移動（"line1\n" = 6コードポイント）
            ctx.window().setPoint(6);
            assertEquals("2", registry.lookup("line-number").orElseThrow().render(ctx));
        }

        @Test
        void column_number_カーソル位置の列番号を0始まりで返す() {
            var ctx = createContext("test.txt", "abcdef");
            ctx.window().setPoint(3);
            assertEquals("3", registry.lookup("column-number").orElseThrow().render(ctx));
        }

        @Test
        void major_mode_メジャーモード名を返す() {
            var ctx = createContext("test.txt", "");
            // デフォルトはTextMode
            assertEquals("text", registry.lookup("major-mode").orElseThrow().render(ctx));
        }

        @Test
        void minor_modes_マイナーモードが無い場合は空文字列を返す() {
            var ctx = createContext("test.txt", "");
            assertEquals("", registry.lookup("minor-modes").orElseThrow().render(ctx));
        }
    }

    @Nested
    class グループ {

        @Test
        void buffer_info_グループはステータスとバッファ名を連結する() {
            var ctx = createContext("main.py", "");
            var result = registry.lookup("buffer-info").orElseThrow().render(ctx);
            // "--\  main.py" (未変更、折り返しモード)
            assertEquals("--\\  main.py", result);
        }

        @Test
        void position_グループは行番号と列番号を括弧で囲んで表示する() {
            var ctx = createContext("test.txt", "hello");
            ctx.window().setPoint(3);
            var result = registry.lookup("position").orElseThrow().render(ctx);
            assertEquals("(1,3)", result);
        }

        @Test
        void mode_info_グループはモード名を括弧で囲んで表示する() {
            var ctx = createContext("test.txt", "");
            var result = registry.lookup("mode-info").orElseThrow().render(ctx);
            assertEquals("(text)", result);
        }

        @Test
        void misc_info_グループは子要素なしで空文字列を返す() {
            var ctx = createContext("test.txt", "");
            var result = registry.lookup("misc-info").orElseThrow().render(ctx);
            assertEquals("", result);
        }
    }

    @Nested
    class デフォルトフォーマット統合 {

        @Test
        void デフォルトフォーマットで従来のモードライン相当の文字列が生成される() {
            var settingsRegistry = new SettingsRegistry();
            var warningBuffer =
                    new io.github.shomah4a.alle.core.buffer.MessageBuffer("*Warnings*", 100, settingsRegistry);
            var renderer = new StatusLineRenderer(registry, warningBuffer);

            var ctx = createContext("test.txt", "hello\nworld");
            ctx.window().setPoint(8); // 2行目の "or" の 'r' (index=8: "hello\nwo" = 8)

            var format = StatusLineFormat.defaultFormat();
            var result = renderer.render(format.entries(), ctx);

            // "----\  test.txt    (2,2)  (text) "
            assertEquals("----\\  test.txt    (2,2)  (text) ", result);
        }
    }
}
