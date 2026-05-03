package io.github.shomah4a.alle.core.mode.modes.shell;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.input.DefaultInteractiveShellProcess;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;

/**
 * シェルモードのコマンド群・キーマップ・CommandResolver登録を行う。
 */
public final class ShellInitializer {

    private ShellInitializer() {}

    /**
     * シェルモードのコマンド群・キーマップを構築し、CommandResolverへ登録する。
     *
     * @return グローバルレジストリへ登録すべきShellCommand
     */
    public static ShellCommand initialize(
            CommandRegistry globalRegistry, CommandResolver commandResolver, SettingsRegistry settingsRegistry) {

        var sendInputCmd = new ShellSendInputCommand();
        var interruptCmd = new ShellInterruptCommand();
        var suspendCmd = new ShellSuspendCommand();

        var shellCommandRegistry = new CommandRegistry();
        shellCommandRegistry.register(sendInputCmd);
        shellCommandRegistry.register(interruptCmd);
        shellCommandRegistry.register(suspendCmd);
        commandResolver.registerModeCommands("shell", shellCommandRegistry);

        var shellKeymap = createKeymap(sendInputCmd, interruptCmd, suspendCmd);

        ShellProcessFactory processFactory = DefaultInteractiveShellProcess::start;

        return new ShellCommand(processFactory, shellKeymap, shellCommandRegistry, settingsRegistry);
    }

    private static Keymap createKeymap(
            ShellSendInputCommand sendInputCmd, ShellInterruptCommand interruptCmd, ShellSuspendCommand suspendCmd) {
        var keymap = new Keymap("shell");

        // RET: 入力送信（改行の代わり）
        keymap.bind(KeyStroke.of('\n'), sendInputCmd);

        // C-c プレフィックス
        var ctrlCMap = new Keymap("shell-C-c");
        ctrlCMap.bind(KeyStroke.ctrl('c'), interruptCmd);
        ctrlCMap.bind(KeyStroke.ctrl('z'), suspendCmd);
        keymap.bindPrefix(KeyStroke.ctrl('c'), ctrlCMap);

        return keymap;
    }
}
