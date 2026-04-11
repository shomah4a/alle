package io.github.shomah4a.alle.core.mode.modes.dired.git;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.modes.dired.DiredCustomColumn;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredBufferUpdater;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMode;
import io.github.shomah4a.alle.core.styling.FaceName;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MapIterable;

/**
 * tree-dired-git マイナーモード。
 * tree-dired バッファにgit情報（ステータスカラム、ブランチ名）を追加する。
 */
public class TreeDiredGitMode implements MinorMode {

    private final GitStatusProvider gitStatusProvider;
    private final Keymap keymap;
    private final CommandRegistry commandRegistry;

    public TreeDiredGitMode(GitStatusProvider gitStatusProvider, Keymap keymap, CommandRegistry commandRegistry) {
        this.gitStatusProvider = gitStatusProvider;
        this.keymap = keymap;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public String name() {
        return "tree-dired-git";
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
    public void onEnable(BufferFacade buffer) {
        MajorMode majorMode = buffer.getMajorMode();
        if (!(majorMode instanceof TreeDiredMode diredMode)) {
            return;
        }
        Path rootDirectory = diredMode.getModel().getRootDirectory();
        setupCustomColumn(buffer, rootDirectory);
        setupRootSuffix(buffer, rootDirectory);
        setupRefreshHook(buffer);
    }

    @Override
    public void onDisable(BufferFacade buffer) {
        buffer.removeVariable(TreeDiredBufferUpdater.CUSTOM_COLUMNS_KEY);
        buffer.removeVariable(TreeDiredBufferUpdater.ROOT_SUFFIX_KEY);
        buffer.removeVariable(TreeDiredBufferUpdater.REFRESH_HOOKS_KEY);
    }

    /**
     * git情報を再取得してバッファ変数を更新する。
     */
    void refresh(BufferFacade buffer) {
        MajorMode majorMode = buffer.getMajorMode();
        if (!(majorMode instanceof TreeDiredMode diredMode)) {
            return;
        }
        Path rootDirectory = diredMode.getModel().getRootDirectory();
        setupCustomColumn(buffer, rootDirectory);
        setupRootSuffix(buffer, rootDirectory);
    }

    GitStatusProvider getGitStatusProvider() {
        return gitStatusProvider;
    }

    private void setupCustomColumn(BufferFacade buffer, Path rootDirectory) {
        MapIterable<Path, String> statuses = gitStatusProvider.getFileStatuses(rootDirectory);
        var column = new DiredCustomColumn() {
            @Override
            public String header() {
                return "git";
            }

            @Override
            public String renderCell(Path path) {
                return statuses.getIfAbsentValue(path, "");
            }

            @Override
            public Optional<FaceName> face(Path path) {
                String status = statuses.getIfAbsentValue(path, "");
                return switch (status) {
                    case "A" -> Optional.of(FaceName.DIFF_ADDED);
                    case "M" -> Optional.of(FaceName.DIFF_MODIFIED);
                    case "D" -> Optional.of(FaceName.DIFF_DELETED);
                    default -> Optional.empty();
                };
            }
        };
        buffer.setVariable(TreeDiredBufferUpdater.CUSTOM_COLUMNS_KEY, Lists.immutable.of(column));
    }

    private void setupRootSuffix(BufferFacade buffer, Path rootDirectory) {
        String branch = gitStatusProvider.getBranch(rootDirectory);
        if (!branch.isEmpty()) {
            buffer.setVariable(TreeDiredBufferUpdater.ROOT_SUFFIX_KEY, "[" + branch + "]");
        }
    }

    private void setupRefreshHook(BufferFacade buffer) {
        Runnable hook = () -> refresh(buffer);
        buffer.setVariable(TreeDiredBufferUpdater.REFRESH_HOOKS_KEY, Lists.immutable.of(hook));
    }
}
