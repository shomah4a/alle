package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.list.ListIterable;

/**
 * マーク済みまたはカーソル行のエントリのパーミッションを変更する。
 * パーミッション文字列（例: "rwxr-xr-x"）をミニバッファで入力させる。
 */
public class TreeDiredChmodCommand implements Command, Loggable {

    private final FileOperations fileOperations;
    private final InputHistory chmodHistory;

    public TreeDiredChmodCommand(FileOperations fileOperations, InputHistory chmodHistory) {
        this.fileOperations = fileOperations;
        this.chmodHistory = chmodHistory;
    }

    @Override
    public String name() {
        return "tree-dired-chmod";
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

        String prompt = "chmod (e.g. rwxr-xr-x): ";
        return context.inputPrompter().prompt(prompt, chmodHistory).thenAccept(result -> {
            if (!(result instanceof PromptResult.Confirmed confirmed)) {
                return;
            }
            String permissions = confirmed.value().trim();
            executeChmod(context, diredMode, targets, permissions);
        });
    }

    private void executeChmod(
            CommandContext context, TreeDiredMode diredMode, ListIterable<TreeDiredEntry> targets, String permissions) {
        int successCount = 0;
        for (TreeDiredEntry entry : targets) {
            try {
                fileOperations.setPermissions(entry.path(), permissions);
                successCount++;
            } catch (IOException e) {
                logger().warn("chmod に失敗: " + entry.path(), e);
                context.handleError("chmod に失敗: " + entry.path().getFileName() + " - " + e.getMessage(), e);
            } catch (IllegalArgumentException e) {
                context.handleError("不正なパーミッション文字列: " + permissions, e);
                return;
            }
        }
        if (successCount > 0) {
            diredMode.getModel().clearMarks();
            TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
            context.messageBuffer().message(successCount + " 件のパーミッションを変更しました");
        }
    }
}
