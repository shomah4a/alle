package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import java.util.Optional;

/**
 * git-log 結果バッファのメジャーモード。
 * バッファは read-only 前提。q キーで kill-buffer するキーマップを持つ。
 * 追加取得型ページネーションのため {@link GitLogModel} を保持する。
 */
public class GitLogMode implements MajorMode {

    private final GitLogModel model;
    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    public GitLogMode(GitLogModel model, Keymap keymap, CommandRegistry commandRegistry) {
        this.model = model;
        this.keymap = keymap;
        this.commandRegistry = commandRegistry;
    }

    public GitLogModel getModel() {
        return model;
    }

    @Override
    public String name() {
        return "git-log";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.of(keymap);
    }

    @Override
    public Optional<CommandRegistry> commandRegistry() {
        return Optional.of(commandRegistry);
    }
}
