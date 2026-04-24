package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.FileOpenService;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredCommand;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

/**
 * ファイルを開くコマンド。
 * InputPrompterでファイルパスを入力させ、FileOpenServiceでファイルを読み込む。
 * 同一パスのバッファが既に存在する場合はそのバッファに切り替える。
 * ファイルが存在しない場合は空バッファをファイルパス付きで作成する。
 * ディレクトリが指定された場合はTree Diredで開く。
 */
public class FindFileCommand implements Command {

    private final FileOpenService fileOpenService;
    private final Path workingDirectory;
    private final InputHistory filePathHistory;
    private final Predicate<Path> directoryChecker;
    private final FilePathInputPrompter filePathInputPrompter;
    private @Nullable TreeDiredCommand treeDiredCommand;

    public FindFileCommand(
            FileOpenService fileOpenService,
            Path workingDirectory,
            InputHistory filePathHistory,
            Predicate<Path> directoryChecker,
            FilePathInputPrompter filePathInputPrompter) {
        this.fileOpenService = fileOpenService;
        this.workingDirectory = workingDirectory;
        this.filePathHistory = filePathHistory;
        this.directoryChecker = directoryChecker;
        this.filePathInputPrompter = filePathInputPrompter;
    }

    /**
     * Tree Dired コマンドを設定する。
     * ディレクトリを指定された場合にTree Diredで開くために必要。
     */
    public void setTreeDiredCommand(TreeDiredCommand treeDiredCommand) {
        this.treeDiredCommand = treeDiredCommand;
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
                        openFile(context, confirmed.value());
                    }
                });
    }

    private void openFile(CommandContext context, String pathString) {
        if (pathString.isEmpty()) {
            return;
        }
        // 末尾の "/" を除去して正規化（~ 展開は FilePathInputPrompter が済ませている）
        String trimmed = pathString.endsWith("/") ? pathString.substring(0, pathString.length() - 1) : pathString;
        var path = FileOpenService.normalizePath(trimmed);

        // ディレクトリの場合はTree Diredで開く
        if (treeDiredCommand != null && directoryChecker.test(path)) {
            treeDiredCommand.openDiredForPath(context, path);
            return;
        }

        fileOpenService.openFile(trimmed, context.bufferManager(), context.frame());
    }
}
