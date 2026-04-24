package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.PathOpenService;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * ファイルを開くコマンド。
 * InputPrompterでファイルパスを入力させ、PathOpenServiceでパスを開く。
 * ファイルの場合はバッファに読み込み、ディレクトリの場合はTree Diredで開く。
 */
public class FindFileCommand implements Command {

    private final PathOpenService pathOpenService;
    private final Path workingDirectory;
    private final InputHistory filePathHistory;
    private final FilePathInputPrompter filePathInputPrompter;

    public FindFileCommand(
            PathOpenService pathOpenService,
            Path workingDirectory,
            InputHistory filePathHistory,
            FilePathInputPrompter filePathInputPrompter) {
        this.pathOpenService = pathOpenService;
        this.workingDirectory = workingDirectory;
        this.filePathHistory = filePathHistory;
        this.filePathInputPrompter = filePathInputPrompter;
    }

    @Override
    public String name() {
        return "find-file";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var defaultDir = context.activeWindow().getBuffer().getDefaultDirectory(workingDirectory);
        return filePathInputPrompter
                .prompt("Find file: ", defaultDir.toString(), filePathHistory)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        pathOpenService.open(confirmed.value(), context.bufferManager(), context.frame());
                    }
                });
    }
}
