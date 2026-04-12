package io.github.shomah4a.alle.core.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatusLineRendererTest {

    private StatusLineRegistry registry;
    private MessageBuffer warningBuffer;
    private StatusLineRenderer renderer;
    private StatusLineContext context;

    @BeforeEach
    void setUp() {
        var settingsRegistry = new SettingsRegistry();
        registry = new StatusLineRegistry();
        warningBuffer = new MessageBuffer("*Warnings*", 100, settingsRegistry);
        renderer = new StatusLineRenderer(registry, warningBuffer);

        var buffer = new BufferFacade(new TextBuffer("test.txt", new GapTextModel(), settingsRegistry));
        var window = new Window(buffer);
        context = new StatusLineContext(window, buffer);
    }

    @Test
    void リテラルのみのフォーマットはそのまま表示される() {
        var format = Lists.immutable.of(
                StatusLineFormatEntry.literal("--"),
                StatusLineFormatEntry.literal("  "),
                StatusLineFormatEntry.literal("hello"));

        assertEquals("--  hello", renderer.render(format, context));
    }

    @Test
    void スロット参照がレジストリから解決されてrenderされる() {
        registry.register(new StatusLineSlot("buf-name", ctx -> ctx.buffer().getName()));

        var format = Lists.immutable.of(
                StatusLineFormatEntry.literal("["),
                StatusLineFormatEntry.slotRef("buf-name"),
                StatusLineFormatEntry.literal("]"));

        assertEquals("[test.txt]", renderer.render(format, context));
    }

    @Test
    void 未登録スロット名は空文字列として扱われ警告が出力される() {
        var format = Lists.immutable.of(
                StatusLineFormatEntry.literal("before-"),
                StatusLineFormatEntry.slotRef("unknown-slot"),
                StatusLineFormatEntry.literal("-after"));

        assertEquals("before--after", renderer.render(format, context));
        assertEquals(
                "ステータスライン: 未登録のスロット名 'unknown-slot'",
                warningBuffer.getLastMessage().orElse(""));
    }

    @Test
    void グループ参照がレジストリから解決されて子要素が連結される() {
        var group = new StatusLineGroup("info");
        group.addChild(new StatusLineSlot("a", ctx -> "A"));
        group.addChild(new StatusLineSlot("b", ctx -> "B"));
        registry.register(group);

        var format = Lists.immutable.of(
                StatusLineFormatEntry.literal("("),
                StatusLineFormatEntry.slotRef("info"),
                StatusLineFormatEntry.literal(")"));

        assertEquals("(AB)", renderer.render(format, context));
    }

    @Test
    void 空のフォーマットは空文字列を返す() {
        var format = Lists.immutable.<StatusLineFormatEntry>empty();
        assertEquals("", renderer.render(format, context));
    }

    @Test
    void リテラルとスロット参照の混在フォーマット() {
        registry.register(new StatusLineSlot("mode", ctx -> "Text"));
        registry.register(new StatusLineSlot("line", ctx -> "1"));

        var format = Lists.immutable.of(
                StatusLineFormatEntry.literal("--**  "),
                StatusLineFormatEntry.slotRef("mode"),
                StatusLineFormatEntry.literal(" L"),
                StatusLineFormatEntry.slotRef("line"));

        assertEquals("--**  Text L1", renderer.render(format, context));
    }
}
