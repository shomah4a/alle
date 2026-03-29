package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * マーク済みまたはカーソル行のエントリをコピーする。
 * 複数エントリの場合はコピー先ディレクトリを、単一エントリの場合はコピー先パスを入力させる。
 * 対象にディレクトリが含まれる場合は再帰コピーの確認を行う。
 */
public class TreeDiredCopyCommand implements Command {

    private static final Logger logger = Logger.getLogger(TreeDiredCopyCommand.class.getName());

    private final FileOperations fileOperations;
    private final InputHistory copyHistory;
    private final InputHistory confirmHistory;

    public TreeDiredCopyCommand(FileOperations fileOperations, InputHistory copyHistory, InputHistory confirmHistory) {
        this.fileOperations = fileOperations;
        this.copyHistory = copyHistory;
        this.confirmHistory = confirmHistory;
    }

    @Override
    public String name() {
        return "tree-dired-copy";
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

        String prompt;
        String initialValue;
        if (targets.size() == 1) {
            prompt = "Copy " + targets.get(0).path().getFileName() + " to: ";
            Path parent = targets.get(0).path().getParent();
            initialValue = (parent != null
                            ? parent.toString()
                            : diredMode.getModel().getRootDirectory().toString()) + "/";
        } else {
            prompt = "Copy " + targets.size() + " files to directory: ";
            initialValue = diredMode.getModel().getRootDirectory().toString() + "/";
        }

        return context.inputPrompter()
                .prompt(prompt, initialValue, copyHistory, text -> Lists.immutable.empty())
                .thenCompose(result -> {
                    if (!(result instanceof PromptResult.Confirmed confirmed)) {
                        return CompletableFuture.completedFuture(null);
                    }
                    var targetPath = Path.of(confirmed.value()).toAbsolutePath().normalize();

                    boolean hasDirectories = targets.anySatisfy(TreeDiredEntry::isDirectory);
                    if (hasDirectories) {
                        return context.inputPrompter()
                                .prompt("Includes directories. Copy recursive? (y/n): ", confirmHistory)
                                .thenAccept(confirmResult -> {
                                    if (confirmResult instanceof PromptResult.Confirmed c && isYes(c.value())) {
                                        executeCopy(context, diredMode, targets, targetPath);
                                    }
                                });
                    } else {
                        executeCopy(context, diredMode, targets, targetPath);
                        return CompletableFuture.completedFuture(null);
                    }
                });
    }

    private void executeCopy(
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
                fileOperations.copy(entry.path(), destination);
                successCount++;
            } catch (IOException e) {
                logger.log(Level.WARNING, "コピーに失敗: " + entry.path(), e);
                context.handleError("コピーに失敗: " + entry.path().getFileName() + " - " + e.getMessage(), e);
            }
        }
        if (successCount > 0) {
            diredMode.getModel().clearMarks();
            TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
            context.messageBuffer().message(successCount + " 件コピーしました");
        }
    }

    private static boolean isYes(String value) {
        String v = value.trim().toLowerCase(Locale.ROOT);
        return "y".equals(v) || "yes".equals(v);
    }
}
