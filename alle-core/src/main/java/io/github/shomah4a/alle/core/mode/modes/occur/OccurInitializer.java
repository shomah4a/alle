package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;

/**
 * Occurモードのコマンド群・キーマップ・CommandResolver登録を行う。
 */
public final class OccurInitializer {

    private OccurInitializer() {}

    /**
     * Occurモードのコマンド群・キーマップを構築し、CommandResolverへ登録する。
     *
     * @return グローバルレジストリへ登録すべきOccurCommand
     */
    public static OccurCommand initialize(
            CommandRegistry globalRegistry, CommandResolver commandResolver, SettingsRegistry settingsRegistry) {

        var nextLineCmd = globalRegistry.lookup("next-line").orElseThrow();
        var previousLineCmd = globalRegistry.lookup("previous-line").orElseThrow();
        var occurNextLineCommand = new OccurNextLineCommand(nextLineCmd);
        var occurPreviousLineCommand = new OccurPreviousLineCommand(previousLineCmd);
        var occurGotoLineCommand = new OccurGotoLineCommand();
        var occurQuitCommand = new OccurQuitCommand();
        var occurNoOpCommand = new OccurNoOpCommand();
        var killBufferCmd = globalRegistry.lookup("kill-buffer").orElseThrow();

        var occurCommandRegistry = new CommandRegistry();
        occurCommandRegistry.register(occurNextLineCommand);
        occurCommandRegistry.register(occurPreviousLineCommand);
        occurCommandRegistry.register(occurGotoLineCommand);
        occurCommandRegistry.register(occurQuitCommand);
        occurCommandRegistry.register(occurNoOpCommand);
        commandResolver.registerModeCommands("occur", occurCommandRegistry);

        var occurKeymap = createKeymap(
                occurNextLineCommand,
                occurPreviousLineCommand,
                occurGotoLineCommand,
                occurQuitCommand,
                occurNoOpCommand,
                killBufferCmd);

        var occurHistory = new InputHistory();
        return new OccurCommand(occurHistory, occurKeymap, occurCommandRegistry, settingsRegistry);
    }

    private static Keymap createKeymap(
            OccurNextLineCommand occurNextLineCommand,
            OccurPreviousLineCommand occurPreviousLineCommand,
            OccurGotoLineCommand occurGotoLineCommand,
            OccurQuitCommand occurQuitCommand,
            OccurNoOpCommand occurNoOpCommand,
            io.github.shomah4a.alle.core.command.Command killBufferCommand) {
        var keymap = new Keymap("occur");
        keymap.setDefaultCommand(occurNoOpCommand);
        keymap.bind(KeyStroke.of('\n'), occurGotoLineCommand);
        keymap.bind(KeyStroke.ctrl('n'), occurNextLineCommand);
        keymap.bind(KeyStroke.ctrl('p'), occurPreviousLineCommand);
        keymap.bind(KeyStroke.of(KeyStroke.ARROW_DOWN), occurNextLineCommand);
        keymap.bind(KeyStroke.of(KeyStroke.ARROW_UP), occurPreviousLineCommand);
        keymap.bind(KeyStroke.of('q'), occurQuitCommand);
        keymap.bind(KeyStroke.of('k'), killBufferCommand);
        return keymap;
    }
}
