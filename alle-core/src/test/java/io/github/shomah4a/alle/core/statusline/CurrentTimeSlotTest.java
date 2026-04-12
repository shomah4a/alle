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

    private static StatusLineContext createContext() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        return new StatusLineContext(window, buffer);
    }

    @Test
    void 固定時刻をフォーマットして返す() {
        var fixedInstant = Instant.parse("2026-04-12T09:30:45Z");
        var clock = Clock.fixed(fixedInstant, ZoneId.of("Asia/Tokyo"));
        var slot = new CurrentTimeSlot(clock);
        var ctx = createContext();

        // Asia/Tokyo = UTC+9 → 18:30:45
        assertEquals(" 2026-04-12 18:30:45", slot.render(ctx));
    }

    @Test
    void name_はcurrent_timeを返す() {
        var slot = new CurrentTimeSlot(Clock.system(ZoneId.of("Asia/Tokyo")));
        assertEquals("current-time", slot.name());
    }
}
