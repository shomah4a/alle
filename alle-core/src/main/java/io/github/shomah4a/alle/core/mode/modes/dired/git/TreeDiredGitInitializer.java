package io.github.shomah4a.alle.core.mode.modes.dired.git;

import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMode;
import java.nio.file.Files;

/**
 * tree-dired-git マイナーモードの初期化を行う。
 */
public final class TreeDiredGitInitializer {

    private TreeDiredGitInitializer() {}

    /**
     * tree-dired-git モードを登録し、tree-dired の major mode hook で自動有効化を設定する。
     */
    public static void initialize(
            ModeRegistry modeRegistry,
            CommandResolver commandResolver,
            GitStatusProvider gitStatusProvider,
            FileOperations fileOperations) {

        var addCommand = new TreeDiredGitAddCommand();
        var deleteCommand = new TreeDiredGitDeleteCommand(gitStatusProvider, fileOperations);
        var renameCommand = new TreeDiredGitRenameCommand(gitStatusProvider, fileOperations);

        var gitCommandRegistry = new CommandRegistry();
        gitCommandRegistry.register(addCommand);
        gitCommandRegistry.register(deleteCommand);
        gitCommandRegistry.register(renameCommand);
        commandResolver.registerModeCommands("tree-dired-git", gitCommandRegistry);

        var gitKeymap = new Keymap("tree-dired-git");
        gitKeymap.bind(KeyStroke.of('A'), addCommand);
        gitKeymap.bind(KeyStroke.of('D'), deleteCommand);
        gitKeymap.bind(KeyStroke.of('R'), renameCommand);

        modeRegistry.registerMinorMode(
                "tree-dired-git", () -> new TreeDiredGitMode(gitStatusProvider, gitKeymap, gitCommandRegistry));

        // tree-dired 起動時に .git ディレクトリがあれば自動有効化
        modeRegistry.addMajorModeHook("tree-dired", (buffer, modeName) -> {
            var majorMode = buffer.getMajorMode();
            if (!(majorMode instanceof TreeDiredMode diredMode)) {
                return;
            }
            var rootDir = diredMode.getModel().getRootDirectory();
            if (Files.isDirectory(rootDir.resolve(".git"))) {
                var mode = new TreeDiredGitMode(gitStatusProvider, gitKeymap, gitCommandRegistry);
                buffer.enableMinorMode(mode);
                mode.onEnable(buffer);
                modeRegistry.runMinorModeHooks("tree-dired-git", buffer);
            }
        });
    }
}
