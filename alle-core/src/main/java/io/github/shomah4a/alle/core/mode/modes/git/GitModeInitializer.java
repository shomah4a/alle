package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import java.nio.file.Path;
import java.util.Optional;

/**
 * git-mode マイナーモード / git-log コマンド / git-log メジャーモードの初期化を行う。
 * 共通 hook (addAllMajorModeHook) で git リポジトリ配下のバッファに自動有効化する。
 */
public final class GitModeInitializer {

    private GitModeInitializer() {}

    public static void initialize(
            ModeRegistry modeRegistry,
            CommandResolver commandResolver,
            CommandRegistry globalRegistry,
            GitLogProvider gitLogProvider,
            GitRepositoryLocator locator,
            SettingsRegistry settingsRegistry) {

        // git-log 結果バッファのメジャーモード用コマンドとキーマップ
        var killBufferCmd = globalRegistry.lookup("kill-buffer").orElseThrow();
        var nextPageCmd = new GitLogNextPageCommand(gitLogProvider);

        var gitLogModeRegistry = new CommandRegistry();
        gitLogModeRegistry.register(nextPageCmd);
        commandResolver.registerModeCommands("git-log", gitLogModeRegistry);

        var gitLogKeymap = new Keymap("git-log");
        gitLogKeymap.bind(KeyStroke.of('q'), killBufferCmd);
        gitLogKeymap.bind(KeyStroke.of('n'), nextPageCmd);

        // git-log コマンド本体 (M-x git-log)
        var gitLogCmd =
                new GitLogCommand(gitLogProvider, locator, gitLogKeymap, gitLogModeRegistry, settingsRegistry);

        // git-mode マイナーモード用コマンドとキーマップ
        var gitCommandRegistry = new CommandRegistry();
        gitCommandRegistry.register(gitLogCmd);
        commandResolver.registerModeCommands("git", gitCommandRegistry);

        // グローバルからも M-x で呼べるようにする
        globalRegistry.register(gitLogCmd);
        globalRegistry.register(nextPageCmd);

        var gitKeymap = new Keymap("git");
        // マイナーモード固有キーバインドは現状なし。M-x git-log で起動する。

        modeRegistry.registerMinorMode("git", () -> new GitMode(gitKeymap, gitCommandRegistry));

        // 全メジャーモード共通 hook で git リポジトリ配下なら自動有効化
        modeRegistry.addAllMajorModeHook(
                (buffer, modeName) -> tryAutoEnable(buffer, modeRegistry, locator, gitKeymap, gitCommandRegistry));
    }

    /**
     * 自動有効化 hook 本体。バッファ紐づき Path が git リポジトリ配下なら git-mode を有効化する。
     * hook 経由の呼び出しから直接呼び出せる形にしてテスト可能にしている。
     */
    static void tryAutoEnable(
            BufferFacade buffer,
            ModeRegistry modeRegistry,
            GitRepositoryLocator locator,
            Keymap gitKeymap,
            CommandRegistry gitCommandRegistry) {
        var pathOpt = extractPath(buffer);
        if (pathOpt.isEmpty()) {
            return;
        }
        var repoRootOpt = locator.locate(pathOpt.get());
        if (repoRootOpt.isEmpty()) {
            return;
        }
        var mode = new GitMode(gitKeymap, gitCommandRegistry);
        buffer.enableMinorMode(mode);
        mode.onEnable(buffer);
        modeRegistry.runMinorModeHooks("git", buffer);
    }

    private static Optional<Path> extractPath(BufferFacade buffer) {
        var majorMode = buffer.getMajorMode();
        if (majorMode instanceof TreeDiredMode diredMode) {
            return Optional.of(diredMode.getModel().getRootDirectory());
        }
        return buffer.getFilePath();
    }
}
