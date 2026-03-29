package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.collections.api.list.ListIterable;

/**
 * マーク済みまたはカーソル行のエントリのオーナーを変更する。
 * オーナー名をミニバッファで入力させる。
 */
public class TreeDiredChownCommand implements Command {

    private static final Logger logger = Logger.getLogger(TreeDiredChownCommand.class.getName());

    private final FileOperations fileOperations;
    private final InputHistory chownHistory;

    public TreeDiredChownCommand(FileOperations fileOperations, InputHistory chownHistory) {
        this.fileOperations = fileOperations;
        this.chownHistory = chownHistory;
    }

    @Override
    public String name() {
        return "tree-dired-chown";
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

        String prompt = "chown (owner): ";
        return context.inputPrompter().prompt(prompt, chownHistory).thenAccept(result -> {
            if (!(result instanceof PromptResult.Confirmed confirmed)) {
                return;
            }
            String owner = confirmed.value().trim();
            executeChown(context, diredMode, targets, owner);
        });
    }

    private void executeChown(
            CommandContext context, TreeDiredMode diredMode, ListIterable<TreeDiredEntry> targets, String owner) {
        int successCount = 0;
        for (TreeDiredEntry entry : targets) {
            try {
                fileOperations.setOwner(entry.path(), owner);
                successCount++;
            } catch (IOException e) {
                logger.log(Level.WARNING, "chown に失敗: " + entry.path(), e);
                context.handleError("chown に失敗: " + entry.path().getFileName() + " - " + e.getMessage(), e);
            }
        }
        if (successCount > 0) {
            diredMode.getModel().clearMarks();
            TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
            context.messageBuffer().message(successCount + " 件のオーナーを変更しました");
        }
    }
}
