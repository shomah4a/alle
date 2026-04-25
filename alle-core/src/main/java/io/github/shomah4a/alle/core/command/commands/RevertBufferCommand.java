package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.Completer;
import io.github.shomah4a.alle.core.input.CompletionCandidate;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;

/**
 * バッファの内容をファイルから再読み込みするコマンド。
 * バッファが変更されている場合はユーザーに確認を求める。
 */
public class RevertBufferCommand implements Command, Loggable {

    private static final Completer YES_NO_COMPLETER = input ->
            Lists.immutable.of("yes", "no").select(s -> s.startsWith(input)).collect(CompletionCandidate::terminal);

    private final BufferIO bufferIO;
    private final InputHistory confirmHistory;

    public RevertBufferCommand(BufferIO bufferIO) {
        this.bufferIO = bufferIO;
        this.confirmHistory = new InputHistory();
    }

    @Override
    public String name() {
        return "revert-buffer";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var buffer = context.frame().getActiveWindow().getBuffer();

        if (buffer.getFilePath().isEmpty()) {
            context.messageBuffer().message("Buffer is not visiting a file");
            return CompletableFuture.completedFuture(null);
        }

        if (buffer.isReadOnly()) {
            context.messageBuffer().message("Buffer is read-only: " + buffer.getName());
            return CompletableFuture.completedFuture(null);
        }

        if (buffer.isDirty()) {
            return context.inputPrompter()
                    .prompt("Buffer is modified; revert? (yes or no) ", "", confirmHistory, YES_NO_COMPLETER)
                    .thenAccept(result -> {
                        if (result instanceof PromptResult.Confirmed confirmed && "yes".equals(confirmed.value())) {
                            revertBuffer(context, buffer);
                        }
                    });
        }

        revertBuffer(context, buffer);
        return CompletableFuture.completedFuture(null);
    }

    private void revertBuffer(CommandContext context, BufferFacade buffer) {
        try {
            bufferIO.reload(buffer);
        } catch (IOException e) {
            var message = "バッファの再読み込みに失敗: " + buffer.getName();
            logger().warn(message, e);
            context.handleError(message, e);
            return;
        }

        context.messageBuffer()
                .message("Reverted buffer from " + buffer.getFilePath().orElseThrow());
    }
}
