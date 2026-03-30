package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FilePathCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredCommand;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * ファイルを開くコマンド。
 * InputPrompterでファイルパスを入力させ、BufferIOでファイルを読み込む。
 * 同一パスのバッファが既に存在する場合はそのバッファに切り替える。
 * ファイルが存在しない場合は空バッファをファイルパス付きで作成する。
 * ディレクトリが指定された場合はTree Diredで開く。
 */
public class FindFileCommand implements Command {

    private static final Logger logger = Logger.getLogger(FindFileCommand.class.getName());

    private final BufferIO bufferIO;
    private final DirectoryLister directoryLister;
    private final Path workingDirectory;
    private final AutoModeMap autoModeMap;
    private final ModeRegistry modeRegistry;
    private final InputHistory filePathHistory;
    private final Predicate<Path> directoryChecker;
    private @Nullable TreeDiredCommand treeDiredCommand;

    public FindFileCommand(
            BufferIO bufferIO,
            DirectoryLister directoryLister,
            Path workingDirectory,
            AutoModeMap autoModeMap,
            ModeRegistry modeRegistry,
            InputHistory filePathHistory,
            Predicate<Path> directoryChecker) {
        this.bufferIO = bufferIO;
        this.directoryLister = directoryLister;
        this.workingDirectory = workingDirectory;
        this.autoModeMap = autoModeMap;
        this.modeRegistry = modeRegistry;
        this.filePathHistory = filePathHistory;
        this.directoryChecker = directoryChecker;
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
        var completer = new FilePathCompleter(directoryLister);
        String initialValue = workingDirectory + "/";
        return context.inputPrompter()
                .prompt("Find file: ", initialValue, filePathHistory, completer)
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
        // 末尾の "/" を除去して正規化
        String trimmed = pathString.endsWith("/") ? pathString.substring(0, pathString.length() - 1) : pathString;
        var path = normalizePath(trimmed);

        // ディレクトリの場合はTree Diredで開く
        if (treeDiredCommand != null && directoryChecker.test(path)) {
            treeDiredCommand.openDiredForPath(context, path);
            return;
        }

        // 同一パスのバッファが既に存在する場合はそのバッファに切り替え
        var existingBuffer = context.bufferManager().findByPath(path);
        if (existingBuffer.isPresent()) {
            switchToBuffer(context, existingBuffer.get());
            return;
        }

        // ファイルを読み込むか、存在しなければ空バッファを作成
        BufferFacade bufferFacade;
        try {
            var loadResult = bufferIO.load(path);
            bufferFacade = loadResult.bufferFacade();
        } catch (IOException e) {
            logger.log(Level.FINE, "ファイルが存在しないため空バッファを作成: " + path, e);
            bufferFacade = new BufferFacade(new TextBuffer(
                    path.getFileName().toString(), new GapTextModel(), context.settingsRegistry(), path));
        }

        var majorMode = autoModeMap.resolve(bufferFacade.getName());
        bufferFacade.setMajorMode(majorMode);
        modeRegistry.runMajorModeHooks(majorMode.name(), bufferFacade);
        context.bufferManager().add(bufferFacade);
        switchToBuffer(context, bufferFacade);
    }

    /**
     * パス文字列を絶対パスに変換し正規化する。
     */
    static Path normalizePath(String pathString) {
        return Path.of(pathString).toAbsolutePath().normalize();
    }

    private void switchToBuffer(CommandContext context, BufferFacade buffer) {
        context.frame().getActiveWindow().setBuffer(buffer);
        context.frame().getActiveWindow().setPoint(0);
    }
}
