package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.list.ListIterable;

/**
 * マーク済みまたはカーソル行のエントリを削除する。
 * 対象にディレクトリが含まれる場合は (r)ecursive / (f)iles only / (n)o の3択で確認する。
 * ファイルのみの場合は y/n で確認する。
 */
public class TreeDiredDeleteCommand implements Command, Loggable {

    private final FileOperations fileOperations;
    private final InputHistory confirmHistory;

    public TreeDiredDeleteCommand(FileOperations fileOperations, InputHistory confirmHistory) {
        this.fileOperations = fileOperations;
        this.confirmHistory = confirmHistory;
    }

    @Override
    public String name() {
        return "tree-dired-delete";
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

        return context.inputPrompter().prompt(prompt, confirmHistory).thenAccept(result -> {
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
        int successCount = 0;
        for (TreeDiredEntry entry : targets) {
            if (skipDirectories && entry.isDirectory()) {
                continue;
            }
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
