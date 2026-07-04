package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.setting.ModeSettings;
import java.util.Optional;

/**
 * git-mode マイナーモード。
 * git リポジトリ配下のバッファで有効化され、リポジトリ横断の git 関連コマンドを提供する。
 * このモード自身はバッファ変数を書き込まない (onEnable/onDisable は no-op)。
 */
public class GitMode implements MinorMode {

    private static final ModeSettings DEFAULTS = ModeSettings.builder()
            .set(GitSettings.LOG_PAGE_SIZE, 30)
            .set(GitSettings.LOG_SUBJECT_MAX_WIDTH, 80)
            .set(GitSettings.LOG_BODY_MAX_LINES, 3)
            .build();

    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    public GitMode(Keymap keymap, CommandRegistry commandRegistry) {
        this.keymap = keymap;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String name() {
        return "git";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.of(keymap);
    }

    @Override
    public Optional<CommandRegistry> commandRegistry() {
        return Optional.of(commandRegistry);
    }

    @Override
    public ModeSettings settingDefaults() {
        return DEFAULTS;
    }
}
