package io.github.shomah4a.alle.core.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextModeTest {

    @Test
    void モード名がtextである() {
        var mode = new TextMode();
        assertEquals("text", mode.name());
    }

    @Test
    void キーマップを持たない() {
        var mode = new TextMode();
        assertTrue(mode.keymap().isEmpty());
    }
}
