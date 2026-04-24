package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Tree Dired バッファを開くコマンド。
 * ミニバッファでディレクトリパスを入力させ、DiredOpenServiceに委譲する。
 */
public class TreeDiredCommand implements Command {

    private final DiredOpenService diredOpenService;
    private final Path workingDirectory;
    private final InputHistory directoryHistory;
    private final FilePathInputPrompter filePathInputPrompter;

    public TreeDiredCommand(
            DiredOpenService diredOpenService,
            Path workingDirectory,
            InputHistory directoryHistory,
            FilePathInputPrompter filePathInputPrompter) {
        this.diredOpenService = diredOpenService;
        this.workingDirectory = workingDirectory;
        this.directoryHistory = directoryHistory;
        this.filePathInputPrompter = filePathInputPrompter;
    }

    @Override
    public String name() {
        return "tree-dired";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var defaultDir = context.activeWindow().getBuffer().getDefaultDirectory(workingDirectory);
        return filePathInputPrompter
                .prompt("Dired (directory): ", defaultDir.toString(), directoryHistory)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        diredOpenService.openDired(confirmed.value(), context.bufferManager(), context.frame());
                    }
                });
    }
}
