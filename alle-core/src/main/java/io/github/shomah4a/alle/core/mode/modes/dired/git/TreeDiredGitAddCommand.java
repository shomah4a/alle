package io.github.shomah4a.alle.core.mode.modes.dired.git;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.mode.MinorMode;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredBufferUpdater;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredEntryResolver;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMode;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * マーク済みまたはカーソル行のファイルを git add するコマンド。
 */
public class TreeDiredGitAddCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-git-add";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var buffer = context.activeWindow().getBuffer();
        var majorMode = buffer.getMajorMode();
        if (!(majorMode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        var targets = TreeDiredEntryResolver.resolveTargets(context.activeWindow(), diredMode);
        if (targets.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // MinorMode から GitStatusProvider を取得
        var gitModeOpt = findGitMode(buffer.getMinorModes());
        if (gitModeOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        var gitMode = gitModeOpt.get();

        MutableList<java.nio.file.Path> paths = Lists.mutable.empty();
        for (var entry : targets) {
            paths.add(entry.path());
        }

        gitMode.getGitStatusProvider().stageFiles(diredMode.getModel().getRootDirectory(), paths);

        // ステータスを再取得してバッファを更新
        gitMode.refresh(buffer);
        TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);

        int count = paths.size();
        context.messageBuffer().message("git add: " + count + " file" + (count > 1 ? "s" : "") + " staged");

        return CompletableFuture.completedFuture(null);
    }

    private static Optional<TreeDiredGitMode> findGitMode(
            org.eclipse.collections.api.list.ListIterable<MinorMode> minorModes) {
        for (MinorMode mode : minorModes) {
            if (mode instanceof TreeDiredGitMode gitMode) {
                return Optional.of(gitMode);
            }
        }
        return Optional.empty();
    }
}
