package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FilePathCompleter;
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

    public SaveBufferCommand(BufferIO bufferIO, DirectoryLister directoryLister) {
        this.bufferIO = bufferIO;
        this.directoryLister = directoryLister;
    }

    @Override
    public String name() {
        return "save-buffer";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var buffer = context.frame().getActiveWindow().getBuffer();

        if (buffer.getFilePath().isPresent()) {
            saveBuffer(buffer);
            return CompletableFuture.completedFuture(null);
        }

        // ファイルパス未設定の場合はプロンプトで入力を求める
        var completer = new FilePathCompleter(directoryLister);
        return context.inputPrompter().prompt("Save file: ", completer).thenAccept(result -> {
            if (result instanceof PromptResult.Confirmed confirmed) {
                String pathString = confirmed.value();
                if (!pathString.isEmpty()) {
                    buffer.setFilePath(Path.of(pathString));
                    saveBuffer(buffer);
                }
            }
        });
    }

    private void saveBuffer(Buffer buffer) {
        try {
            bufferIO.save(buffer);
        } catch (IOException e) {
            logger.log(Level.WARNING, "バッファの保存に失敗: " + buffer.getName(), e);
        }
    }
}
