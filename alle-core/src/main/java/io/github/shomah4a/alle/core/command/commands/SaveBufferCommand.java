package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * バッファをファイルに保存するコマンド。
 * バッファにファイルパスが設定されていればそのまま保存する。
 * 未設定の場合はInputPrompterでパスを入力させてから保存する。
 */
public class SaveBufferCommand implements Command, Loggable {

    private final BufferIO bufferIO;
    private final FilePathInputPrompter filePathInputPrompter;
    private final InputHistory filePathHistory;

    public SaveBufferCommand(
            BufferIO bufferIO, FilePathInputPrompter filePathInputPrompter, InputHistory filePathHistory) {
        this.bufferIO = bufferIO;
        this.filePathInputPrompter = filePathInputPrompter;
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
        return filePathInputPrompter.prompt("Save file: ", "", filePathHistory).thenAccept(result -> {
            if (result instanceof PromptResult.Confirmed confirmed) {
                String pathString = confirmed.value();
                if (!pathString.isEmpty()) {
                    buffer.setFilePath(Path.of(pathString));
                    context.bufferManager().recomputeUniquify();
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
            logger().warn(message, e);
            context.handleError(message, e);
        }
    }
}
