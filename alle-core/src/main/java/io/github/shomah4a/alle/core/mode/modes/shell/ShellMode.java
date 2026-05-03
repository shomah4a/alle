package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import java.util.Optional;

/**
 * 対話的シェルバッファのメジャーモード。
 * バッファごとに個別インスタンスを生成し、{@link ShellBufferModel} をフィールドに保持する。
 */
public class ShellMode implements MajorMode {

    private final ShellBufferModel model;
    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    ShellMode(ShellBufferModel model, Keymap keymap, CommandRegistry commandRegistry) {
        this.model = model;
        this.keymap = keymap;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String name() {
        return "shell";
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
    public void onDisable(BufferFacade buffer) {
        model.getProcess().destroy();
    }

    ShellBufferModel getModel() {
        return model;
    }
}
