package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * tree-dired 内でディレクトリを作成するコマンド。
 * カーソル行のエントリが属するディレクトリを初期パスとして入力を受け、
 * 作成後にバッファを更新する。
 */
public class TreeDiredMakeDirectoryCommand implements Command, Loggable {

    private final FileOperations fileOperations;
    private final InputHistory history;
    private final FilePathInputPrompter filePathInputPrompter;

    public TreeDiredMakeDirectoryCommand(
            FileOperations fileOperations, InputHistory history, FilePathInputPrompter filePathInputPrompter) {
        this.fileOperations = fileOperations;
        this.history = history;
        this.filePathInputPrompter = filePathInputPrompter;
    }

    @Override
    public String name() {
        return "tree-dired-make-directory";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        String initialDir = resolveInitialDirectory(context, diredMode);

        return filePathInputPrompter
                .prompt("Create directory: ", initialDir, history)
                .thenAccept(result -> {
                    if (result instanceof io.github.shomah4a.alle.core.input.PromptResult.Confirmed confirmed) {
                        var path = Path.of(confirmed.value()).toAbsolutePath().normalize();
                        try {
                            fileOperations.createDirectories(path);
                            TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
                            context.messageBuffer().message("ディレクトリを作成しました: " + path);
                        } catch (IOException e) {
                            logger().warn("ディレクトリ作成に失敗: " + path, e);
                            context.handleError("ディレクトリ作成に失敗: " + e.getMessage(), e);
                        }
                    }
                });
    }

    private String resolveInitialDirectory(CommandContext context, TreeDiredMode diredMode) {
        var entryOpt = TreeDiredEntryResolver.resolve(context.activeWindow(), diredMode);
        if (entryOpt.isPresent()) {
            var entry = entryOpt.get();
            if (entry.isDirectory()) {
                return entry.path().toString();
            }
            Path parent = entry.path().getParent();
            if (parent != null) {
                return parent.toString();
            }
        }
        return diredMode.getModel().getRootDirectory().toString();
    }
}
