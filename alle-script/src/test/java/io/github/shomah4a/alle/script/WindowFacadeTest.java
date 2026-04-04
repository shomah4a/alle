package io.github.shomah4a.alle.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WindowFacadeTest {

    private WindowFacade facade;

    @BeforeEach
    void setUp() {
        var buffer = new TextBuffer("test.txt", new GapTextModel(), new SettingsRegistry());
        var bufferFacade = new BufferFacade(buffer);
        var window = new Window(bufferFacade);
        facade = new WindowFacade(window);
    }

    @Test
    void mark未設定時にselectedTextは空のOptionalを返す() {
        facade.insert("hello world");
        assertTrue(facade.selectedText().isEmpty());
    }

    @Test
    void 選択範囲があるときにselectedTextは該当テキストを返す() {
        facade.insert("hello world");
        facade.gotoChar(0);
        facade.setMark(5);
        assertEquals("hello", facade.selectedText().orElseThrow());
    }

    @Test
    void markとpointが同じ位置のときにselectedTextは空文字列を返す() {
        facade.insert("hello world");
        facade.gotoChar(3);
        facade.setMark(3);
        assertEquals("", facade.selectedText().orElseThrow());
    }
}
