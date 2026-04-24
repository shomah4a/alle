package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.input.DefaultFileOperations;
import io.github.shomah4a.alle.core.input.DefaultShellCommandExecutor;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import java.nio.file.Path;
import java.time.ZoneId;

/**
 * TreeDiredモードのコマンド群・キーマップ・CommandResolver登録を行う。
 */
public final class TreeDiredInitializer {

    private TreeDiredInitializer() {}

    /**
     * 初期化の結果。
     */
    public record InitResult(TreeDiredCommand treeDiredCommand, DiredOpenService diredOpenService) {}

    /**
     * TreeDiredモードのコマンド群・キーマップを構築し、CommandResolverへ登録する。
     *
     * @return TreeDiredCommandとDiredOpenService
     */
    public static InitResult initialize(
            CommandRegistry globalRegistry,
            CommandResolver commandResolver,
            DirectoryLister directoryLister,
            ModeRegistry modeRegistry,
            SettingsRegistry settingsRegistry,
            FilePathInputPrompter filePathInputPrompter) {

        var zoneId = ZoneId.systemDefault();

        var toggleCommand = new TreeDiredToggleCommand();
        var findFileOrToggleCommand = new TreeDiredFindFileOrToggleCommand();
        var upDirectoryCommand = new TreeDiredUpDirectoryCommand();
        var refreshCommand = new TreeDiredRefreshCommand();
        var markCommand = new TreeDiredMarkCommand();
        var unmarkCommand = new TreeDiredUnmarkCommand();
        var toggleMarkCommand = new TreeDiredToggleMarkCommand();
        var fileOperations = new DefaultFileOperations();
        var diredConfirmHistory = new InputHistory();
        var copyCommand = new TreeDiredCopyCommand(fileOperations, new InputHistory(), diredConfirmHistory);
        var renameCommand = new TreeDiredRenameCommand(fileOperations, new InputHistory());
        var deleteCommand = new TreeDiredDeleteCommand(fileOperations, diredConfirmHistory);
        var chmodCommand = new TreeDiredChmodCommand(fileOperations, new InputHistory());
        var chownCommand = new TreeDiredChownCommand(fileOperations, new InputHistory());
        var shellCommandExecutor = new DefaultShellCommandExecutor();
        var shellCommand = new TreeDiredShellCommand(shellCommandExecutor, new InputHistory());
        var killBufferCmd = globalRegistry.lookup("kill-buffer").orElseThrow();

        var diredCommandRegistry = new CommandRegistry();
        diredCommandRegistry.register(toggleCommand);
        diredCommandRegistry.register(findFileOrToggleCommand);
        diredCommandRegistry.register(upDirectoryCommand);
        diredCommandRegistry.register(refreshCommand);
        diredCommandRegistry.register(markCommand);
        diredCommandRegistry.register(unmarkCommand);
        diredCommandRegistry.register(toggleMarkCommand);
        diredCommandRegistry.register(copyCommand);
        diredCommandRegistry.register(renameCommand);
        diredCommandRegistry.register(deleteCommand);
        diredCommandRegistry.register(chmodCommand);
        diredCommandRegistry.register(chownCommand);
        diredCommandRegistry.register(shellCommand);
        commandResolver.registerModeCommands("tree-dired", diredCommandRegistry);

        var diredKeymap = createKeymap(
                toggleCommand,
                findFileOrToggleCommand,
                upDirectoryCommand,
                refreshCommand,
                killBufferCmd,
                markCommand,
                unmarkCommand,
                toggleMarkCommand,
                copyCommand,
                renameCommand,
                deleteCommand,
                chmodCommand,
                chownCommand,
                shellCommand);

        var diredOpenService = new DiredOpenService(
                directoryLister, zoneId, diredKeymap, diredCommandRegistry, modeRegistry, settingsRegistry);

        var diredHistory = new InputHistory();
        var treeDiredCommand = new TreeDiredCommand(
                diredOpenService, Path.of("").toAbsolutePath(), diredHistory, filePathInputPrompter);

        return new InitResult(treeDiredCommand, diredOpenService);
    }

    private static Keymap createKeymap(
            TreeDiredToggleCommand toggleCommand,
            TreeDiredFindFileOrToggleCommand findFileOrToggleCommand,
            TreeDiredUpDirectoryCommand upDirectoryCommand,
            TreeDiredRefreshCommand refreshCommand,
            io.github.shomah4a.alle.core.command.Command killBufferCommand,
            TreeDiredMarkCommand markCommand,
            TreeDiredUnmarkCommand unmarkCommand,
            TreeDiredToggleMarkCommand toggleMarkCommand,
            TreeDiredCopyCommand copyCommand,
            TreeDiredRenameCommand renameCommand,
            TreeDiredDeleteCommand deleteCommand,
            TreeDiredChmodCommand chmodCommand,
            TreeDiredChownCommand chownCommand,
            TreeDiredShellCommand shellCommand) {
        var keymap = new Keymap("tree-dired");
        keymap.setDefaultCommand(new TreeDiredNoOpCommand());
        keymap.bind(KeyStroke.of('\t'), toggleCommand);
        keymap.bind(KeyStroke.of('\n'), findFileOrToggleCommand);
        keymap.bind(KeyStroke.of('f'), findFileOrToggleCommand);
        keymap.bind(KeyStroke.of('^'), upDirectoryCommand);
        keymap.bind(KeyStroke.of('g'), refreshCommand);
        keymap.bind(KeyStroke.of('q'), killBufferCommand);
        keymap.bind(KeyStroke.of('m'), markCommand);
        keymap.bind(KeyStroke.of('u'), unmarkCommand);
        keymap.bind(KeyStroke.of('t'), toggleMarkCommand);
        keymap.bind(KeyStroke.of('C'), copyCommand);
        keymap.bind(KeyStroke.of('R'), renameCommand);
        keymap.bind(KeyStroke.of('D'), deleteCommand);
        keymap.bind(KeyStroke.of('M'), chmodCommand);
        keymap.bind(KeyStroke.of('O'), chownCommand);
        keymap.bind(KeyStroke.of('!'), shellCommand);
        return keymap;
    }
}
