package io.github.shomah4a.alle.core.mode.modes.dired.git;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredBufferUpdater;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredEntry;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredEntryResolver;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMode;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * git管理下のファイルはgit mvで、管理外は通常のファイル移動で処理する。
 */
public class TreeDiredGitRenameCommand implements Command {

    private static final Logger logger = Logger.getLogger(TreeDiredGitRenameCommand.class.getName());

    private final GitStatusProvider gitStatusProvider;
    private final FileOperations fileOperations;

    public TreeDiredGitRenameCommand(GitStatusProvider gitStatusProvider, FileOperations fileOperations) {
        this.gitStatusProvider = gitStatusProvider;
        this.fileOperations = fileOperations;
    }

    @Override
    public String name() {
        return "tree-dired-git-rename";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        ListIterable<TreeDiredEntry> targets = TreeDiredEntryResolver.resolveTargets(context.activeWindow(), diredMode);
        if (targets.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (!TreeDiredEntryResolver.hasSameParentDirectory(targets)) {
            context.messageBuffer().message("複数ディレクトリにまたがるファイルの移動はできません");
            return CompletableFuture.completedFuture(null);
        }

        String prompt;
        String initialValue;
        if (targets.size() == 1) {
            prompt = "Rename " + targets.get(0).path().getFileName() + " to: ";
            Path parent = targets.get(0).path().getParent();
            initialValue = (parent != null
                            ? parent.toString()
                            : diredMode.getModel().getRootDirectory().toString())
                    + "/";
        } else {
            prompt = "Move " + targets.size() + " files to directory: ";
            initialValue = diredMode.getModel().getRootDirectory().toString() + "/";
        }

        return context.inputPrompter()
                .prompt(prompt, initialValue, new InputHistory(), text -> Lists.immutable.empty())
                .thenAccept(result -> {
                    if (!(result instanceof PromptResult.Confirmed confirmed)) {
                        return;
                    }
                    var targetPath = Path.of(confirmed.value()).toAbsolutePath().normalize();
                    executeRename(context, diredMode, targets, targetPath);
                });
    }

    private void executeRename(
            CommandContext context, TreeDiredMode diredMode, ListIterable<TreeDiredEntry> targets, Path targetPath) {
        var rootDirectory = diredMode.getModel().getRootDirectory();
        int successCount = 0;

        for (TreeDiredEntry entry : targets) {
            Path destination;
            if (targets.size() == 1) {
                destination = targetPath;
            } else {
                destination = targetPath.resolve(entry.path().getFileName());
            }

            try {
                if (gitStatusProvider.isTracked(rootDirectory, entry.path())) {
                    gitStatusProvider.moveFile(rootDirectory, entry.path(), destination);
                } else {
                    fileOperations.move(entry.path(), destination);
                }
                successCount++;
            } catch (IOException e) {
                logger.log(Level.WARNING, "リネームに失敗: " + entry.path(), e);
                context.handleError("リネームに失敗: " + entry.path().getFileName() + " - " + e.getMessage(), e);
            }
        }

        if (successCount > 0) {
            diredMode.getModel().clearMarks();
            TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
            context.messageBuffer().message(successCount + " 件リネームしました");
        }
    }
}
