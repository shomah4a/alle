package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FilePathCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * バッファをファイルに保存するコマンド。
 * バッファにファイルパスが設定されていればそのまま保存する。
 * 未設定の場合はInputPrompterでパスを入力させてから保存する。
 */
public class SaveBufferCommand implements Command {

    private static final Logger logger = Logger.getLogger(SaveBufferCommand.class.getName());

    private final BufferIO bufferIO;
    private final DirectoryLister directoryLister;
    private final InputHistory filePathHistory;

    public SaveBufferCommand(BufferIO bufferIO, DirectoryLister directoryLister, InputHistory filePathHistory) {
        this.bufferIO = bufferIO;
        this.directoryLister = directoryLister;
        this.filePathHistory = filePathHistory;
    }

    @Override
    public String name() {
        return "save-buffer";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var buffer = context.frame().getActiveWindow().getBuffer();

        if (buffer.getFilePath().isPresent()) {
            saveBuffer(buffer, context);
            return CompletableFuture.completedFuture(null);
        }

        // ファイルパス未設定の場合はプロンプトで入力を求める
        var completer = new FilePathCompleter(directoryLister);
        return context.inputPrompter()
                .prompt("Save file: ", "", filePathHistory, completer)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        String pathString = confirmed.value();
                        if (!pathString.isEmpty()) {
                            buffer.setFilePath(Path.of(pathString));
                            saveBuffer(buffer, context);
                        }
                    }
                });
    }

    private void saveBuffer(BufferFacade buffer, CommandContext context) {
        try {
            bufferIO.save(buffer);
            context.messageBuffer().message("Saved: " + buffer.getFilePath().orElseThrow());
        } catch (IOException e) {
            var message = "バッファの保存に失敗: " + buffer.getName();
            logger.log(Level.WARNING, message, e);
            context.handleError(message, e);
        }
    }
}
