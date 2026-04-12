package io.github.shomah4a.alle.core.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Test;

class StatusLineGroupTest {

    private static StatusLineContext createContext() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        return new StatusLineContext(window, buffer);
    }

    @Test
    void 子要素のrender結果が連結される() {
        var group = new StatusLineGroup("test-group");
        group.addChild(new StatusLineSlot("a", ctx -> "AAA"));
        group.addChild(new StatusLineSlot("b", ctx -> "BBB"));

        var context = createContext();
        assertEquals("AAABBB", group.render(context));
    }

    @Test
    void 子要素が空の場合は空文字列を返す() {
        var group = new StatusLineGroup("empty-group");
        var context = createContext();
        assertEquals("", group.render(context));
    }

    @Test
    void 子要素にグループをネストできる() {
        var inner = new StatusLineGroup("inner");
        inner.addChild(new StatusLineSlot("x", ctx -> "X"));
        inner.addChild(new StatusLineSlot("y", ctx -> "Y"));

        var outer = new StatusLineGroup("outer");
        outer.addChild(new StatusLineSlot("a", ctx -> "["));
        outer.addChild(inner);
        outer.addChild(new StatusLineSlot("b", ctx -> "]"));

        var context = createContext();
        assertEquals("[XY]", outer.render(context));
    }

    @Test
    void name_で登録名を返す() {
        var group = new StatusLineGroup("my-group");
        assertEquals("my-group", group.name());
    }

    @Test
    void children_で子要素リストを返す() {
        var group = new StatusLineGroup("g");
        var child1 = new StatusLineSlot("a", ctx -> "");
        var child2 = new StatusLineSlot("b", ctx -> "");
        group.addChild(child1);
        group.addChild(child2);

        assertEquals(2, group.children().size());
        assertEquals(child1, group.children().get(0));
        assertEquals(child2, group.children().get(1));
    }
}
