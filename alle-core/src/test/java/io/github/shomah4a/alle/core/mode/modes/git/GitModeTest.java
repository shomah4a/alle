package io.github.shomah4a.alle.core.mode.modes.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Test;

class GitModeTest {

    private GitMode newMode() {
        return new GitMode(new Keymap("git"), new CommandRegistry());
    }

    private BufferFacade newBuffer() {
        return new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
    }

    @Test
    void モード名はgit() {
        assertEquals("git", newMode().name());
    }

    @Test
    void keymapとcommandRegistryはpresent() {
        var mode = newMode();
        assertTrue(mode.keymap().isPresent());
        assertTrue(mode.commandRegistry().isPresent());
    }

    @Test
    void settingDefaultsにgit_log_page_sizeが含まれる() {
        var settings = newMode().settingDefaults();
        assertEquals(30, settings.get(GitSettings.LOG_PAGE_SIZE).orElseThrow());
    }

    @Test
    void settingDefaultsにgit_log_subject_max_widthが含まれる() {
        var settings = newMode().settingDefaults();
        assertEquals(80, settings.get(GitSettings.LOG_SUBJECT_MAX_WIDTH).orElseThrow());
    }

    @Test
    void settingDefaultsにgit_log_body_max_linesが含まれる() {
        var settings = newMode().settingDefaults();
        assertEquals(3, settings.get(GitSettings.LOG_BODY_MAX_LINES).orElseThrow());
    }

    @Test
    void onEnableとonDisableはバッファ長を変化させない() {
        var mode = newMode();
        var buffer = newBuffer();
        int lengthBefore = buffer.length();

        mode.onEnable(buffer);
        mode.onDisable(buffer);

        assertEquals(lengthBefore, buffer.length());
    }
}
