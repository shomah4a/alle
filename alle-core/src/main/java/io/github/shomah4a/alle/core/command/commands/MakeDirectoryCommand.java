package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * ディレクトリを作成するコマンド。
 * FilePathInputPrompterでパスを入力させ、FileOperations.createDirectories()で再帰的に作成する。
 */
public class MakeDirectoryCommand implements Command, Loggable {

    private final FileOperations fileOperations;
    private final Path workingDirectory;
    private final InputHistory history;
    private final FilePathInputPrompter filePathInputPrompter;

    public MakeDirectoryCommand(
            FileOperations fileOperations,
            Path workingDirectory,
            InputHistory history,
            FilePathInputPrompter filePathInputPrompter) {
        this.fileOperations = fileOperations;
        this.workingDirectory = workingDirectory;
        this.history = history;
        this.filePathInputPrompter = filePathInputPrompter;
    }

    @Override
    public String name() {
        return "make-directory";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var defaultDir = context.activeWindow().getBuffer().getDefaultDirectory(workingDirectory);
        return filePathInputPrompter
                .prompt("Create directory: ", defaultDir.toString(), history)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        var path = Path.of(confirmed.value()).toAbsolutePath().normalize();
                        try {
                            fileOperations.createDirectories(path);
                            context.messageBuffer().message("ディレクトリを作成しました: " + path);
                        } catch (IOException e) {
                            logger().warn("ディレクトリ作成に失敗: " + path, e);
                            context.handleError("ディレクトリ作成に失敗: " + e.getMessage(), e);
                        }
                    }
                });
    }
}
