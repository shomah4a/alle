package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import java.util.Optional;

/**
 * occur のメジャーモード。
 * バッファごとに個別インスタンスを生成し、OccurModel をフィールドに保持する。
 */
public class OccurMode implements MajorMode {

    private final OccurModel model;
    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    public OccurMode(OccurModel model, Keymap keymap, CommandRegistry commandRegistry) {
        this.model = model;
        this.keymap = keymap;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String name() {
        return "occur";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.of(keymap);
    }

    @Override
    public Optional<CommandRegistry> commandRegistry() {
        return Optional.of(commandRegistry);
    }

    public OccurModel getModel() {
        return model;
    }
}
