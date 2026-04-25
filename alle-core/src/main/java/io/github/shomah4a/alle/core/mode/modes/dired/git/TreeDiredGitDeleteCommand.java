package io.github.shomah4a.alle.core.mode.modes.dired.git;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredBufferUpdater;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredEntry;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredEntryResolver;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMode;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * git管理下のファイルはgit rmで、管理外は通常のファイル削除で処理する。
 */
public class TreeDiredGitDeleteCommand implements Command, Loggable {

    private final GitStatusProvider gitStatusProvider;
    private final FileOperations fileOperations;

    public TreeDiredGitDeleteCommand(GitStatusProvider gitStatusProvider, FileOperations fileOperations) {
        this.gitStatusProvider = gitStatusProvider;
        this.fileOperations = fileOperations;
    }

    @Override
    public String name() {
        return "tree-dired-git-delete";
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

        boolean hasDirectories = targets.anySatisfy(TreeDiredEntry::isDirectory);
        String prompt;
        if (hasDirectories) {
            prompt = "Delete " + targets.size() + " item(s) including directories? (r)ecursive / (f)iles only / (n)o: ";
        } else {
            prompt = "Delete " + targets.size() + " item(s)? (y/n): ";
        }

        return context.inputPrompter()
                .prompt(prompt, new io.github.shomah4a.alle.core.input.InputHistory())
                .thenAccept(result -> {
                    if (!(result instanceof PromptResult.Confirmed confirmed)) {
                        return;
                    }
                    String value = confirmed.value().trim().toLowerCase(Locale.ROOT);

                    if (hasDirectories) {
                        if ("r".equals(value) || "recursive".equals(value)) {
                            executeDelete(context, diredMode, targets, false);
                        } else if ("f".equals(value) || "files only".equals(value)) {
                            executeDelete(context, diredMode, targets, true);
                        }
                    } else {
                        if ("y".equals(value) || "yes".equals(value)) {
                            executeDelete(context, diredMode, targets, false);
                        }
                    }
                });
    }

    private void executeDelete(
            CommandContext context,
            TreeDiredMode diredMode,
            ListIterable<TreeDiredEntry> targets,
            boolean skipDirectories) {
        var rootDirectory = diredMode.getModel().getRootDirectory();
        var trackedTargets = Lists.mutable.<TreeDiredEntry>empty();
        var untrackedTargets = Lists.mutable.<TreeDiredEntry>empty();

        for (TreeDiredEntry entry : targets) {
            if (skipDirectories && entry.isDirectory()) {
                continue;
            }
            if (gitStatusProvider.isTracked(rootDirectory, entry.path())) {
                trackedTargets.add(entry);
            } else {
                untrackedTargets.add(entry);
            }
        }

        int successCount = 0;

        // git管理下のファイルはgit rmで削除
        if (trackedTargets.notEmpty()) {
            var paths = trackedTargets.collect(TreeDiredEntry::path);
            boolean hasTrackedDirs = trackedTargets.anySatisfy(TreeDiredEntry::isDirectory);
            gitStatusProvider.removeFiles(rootDirectory, paths, hasTrackedDirs);
            successCount += trackedTargets.size();
        }

        // 管理外のファイルは通常削除
        for (TreeDiredEntry entry : untrackedTargets) {
            try {
                fileOperations.delete(entry.path());
                successCount++;
            } catch (IOException e) {
                logger().warn("削除に失敗: " + entry.path(), e);
                context.handleError("削除に失敗: " + entry.path().getFileName() + " - " + e.getMessage(), e);
            }
        }

        if (successCount > 0) {
            diredMode.getModel().clearMarks();
            TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
            context.messageBuffer().message(successCount + " 件削除しました");
        }
    }
}
