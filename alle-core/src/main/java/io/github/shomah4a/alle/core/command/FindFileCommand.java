package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FilePathCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ファイルを開くコマンド。
 * InputPrompterでファイルパスを入力させ、BufferIOでファイルを読み込む。
 * 同一パスのバッファが既に存在する場合はそのバッファに切り替える。
 * ファイルが存在しない場合は空バッファをファイルパス付きで作成する。
 */
public class FindFileCommand implements Command {

    private static final Logger logger = Logger.getLogger(FindFileCommand.class.getName());

    private final BufferIO bufferIO;
    private final DirectoryLister directoryLister;
    private final Path workingDirectory;
    private final AutoModeMap autoModeMap;
    private final ModeRegistry modeRegistry;
    private final InputHistory filePathHistory;

    public FindFileCommand(
            BufferIO bufferIO,
            DirectoryLister directoryLister,
            Path workingDirectory,
            AutoModeMap autoModeMap,
            ModeRegistry modeRegistry,
            InputHistory filePathHistory) {
        this.bufferIO = bufferIO;
        this.directoryLister = directoryLister;
        this.workingDirectory = workingDirectory;
        this.autoModeMap = autoModeMap;
        this.modeRegistry = modeRegistry;
        this.filePathHistory = filePathHistory;
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
        var path = normalizePath(pathString);

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
            bufferFacade =
                    new BufferFacade(new EditableBuffer(path.getFileName().toString(), new GapTextModel(), path));
        }

        var majorMode = autoModeMap.resolve(bufferFacade.getName());
        bufferFacade.setMajorMode(majorMode);
        modeRegistry.runMajorModeHooks(majorMode.name());
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
