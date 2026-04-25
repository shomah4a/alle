package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * マーク済みまたはカーソル行のエントリをリネーム（移動）する。
 * 複数エントリの場合は移動先ディレクトリを、単一エントリの場合は移動先パスを入力させる。
 */
public class TreeDiredRenameCommand implements Command, Loggable {

    private final FileOperations fileOperations;
    private final InputHistory renameHistory;

    public TreeDiredRenameCommand(FileOperations fileOperations, InputHistory renameHistory) {
        this.fileOperations = fileOperations;
        this.renameHistory = renameHistory;
    }

    @Override
    public String name() {
        return "tree-dired-rename";
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
                            : diredMode.getModel().getRootDirectory().toString()) + "/";
        } else {
            prompt = "Move " + targets.size() + " files to directory: ";
            initialValue = diredMode.getModel().getRootDirectory().toString() + "/";
        }

        return context.inputPrompter()
                .prompt(prompt, initialValue, renameHistory, text -> Lists.immutable.empty())
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
        int successCount = 0;
        for (TreeDiredEntry entry : targets) {
            Path destination;
            if (targets.size() == 1) {
                destination = targetPath;
            } else {
                destination = targetPath.resolve(entry.path().getFileName());
            }
            try {
                fileOperations.move(entry.path(), destination);
                successCount++;
            } catch (IOException e) {
                logger().warn("リネームに失敗: " + entry.path(), e);
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
