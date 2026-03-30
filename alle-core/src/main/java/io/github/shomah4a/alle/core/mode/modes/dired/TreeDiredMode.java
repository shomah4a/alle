package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Tree Dired のメジャーモード。
 * バッファごとに個別インスタンスを生成し、TreeDiredModel をフィールドに保持する。
 */
public class TreeDiredMode implements MajorMode {

    private final TreeDiredModel model;
    private final Keymap keymap;
    private final ZoneId zoneId;
    private final CommandRegistry commandRegistry;

    public TreeDiredMode(TreeDiredModel model, Keymap keymap, ZoneId zoneId, CommandRegistry commandRegistry) {
        this.model = model;
        this.keymap = keymap;
        this.zoneId = zoneId;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String name() {
        return "tree-dired";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.of(keymap);
    }

    /**
     * このモードに紐づくTreeDiredModelを返す。
     */
    public TreeDiredModel getModel() {
        return model;
    }

    /**
     * タイムスタンプ表示に使用するタイムゾーンを返す。
     */
    public ZoneId getZoneId() {
        return zoneId;
    }

    @Override
    public Optional<CommandRegistry> commandRegistry() {
        return Optional.of(commandRegistry);
    }
}
