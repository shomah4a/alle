package io.github.shomah4a.alle.core.server;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MinorMode;
import java.util.Optional;

/**
 * server マイナーモード。
 * クライアントから依頼されたバッファに付与される。
 * C-x # に server-edit コマンドをバインドする。
 */
public class ServerMinorMode implements MinorMode {

    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    /**
     * ServerMinorMode を構築する。
     *
     * @param serverEditCommand server-edit コマンド
     */
    public ServerMinorMode(ServerEditCommand serverEditCommand) {
        this.commandRegistry = new CommandRegistry();
        commandRegistry.register(serverEditCommand);

        // C-x # に server-edit をバインド
        var ctrlXMap = new Keymap("server:C-x");
        ctrlXMap.bind(KeyStroke.of('#'), serverEditCommand);

        this.keymap = new Keymap("server");
        keymap.bindPrefix(KeyStroke.ctrl('x'), ctrlXMap);
    }

    @Override
    public String name() {
        return "server";
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
