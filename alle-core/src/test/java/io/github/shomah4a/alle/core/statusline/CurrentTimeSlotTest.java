package io.github.shomah4a.alle.core.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CurrentTimeSlotTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-04-12T09:30:45Z");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    private static StatusLineContext createContext() {
        var settingsRegistry = new SettingsRegistry();
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), settingsRegistry));
        var window = new Window(buffer);
        return new StatusLineContext(window, buffer);
    }

    private static StatusLineContext createContextWithCustomFormat(String format) {
        var settingsRegistry = new SettingsRegistry();
        settingsRegistry.register(StatusLineSettings.TIME_FORMAT);
        settingsRegistry.setGlobal(StatusLineSettings.TIME_FORMAT, format);
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), settingsRegistry));
        var window = new Window(buffer);
        return new StatusLineContext(window, buffer);
    }

    @Test
    void デフォルトフォーマットは分までを表示する() {
        var clock = Clock.fixed(FIXED_INSTANT, TOKYO);
        var slot = new CurrentTimeSlot(clock);
        var ctx = createContext();

        // Asia/Tokyo = UTC+9 → 18:30
        assertEquals(" 2026-04-12 18:30", slot.render(ctx));
    }

    @Test
    void グローバル設定でフォーマットを変更できる() {
        var clock = Clock.fixed(FIXED_INSTANT, TOKYO);
        var slot = new CurrentTimeSlot(clock);
        var ctx = createContextWithCustomFormat("HH:mm:ss");

        assertEquals(" 18:30:45", slot.render(ctx));
    }

    @Test
    void name_はcurrent_timeを返す() {
        var slot = new CurrentTimeSlot(Clock.system(TOKYO));
        assertEquals("current-time", slot.name());
    }
}
