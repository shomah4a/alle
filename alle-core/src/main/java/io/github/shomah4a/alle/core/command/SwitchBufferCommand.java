package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.input.BufferNameCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.util.concurrent.CompletableFuture;

/**
 * バッファを切り替えるコマンド。
 * ミニバッファでバッファ名を入力させ、該当バッファに切り替える。
 * 直前のバッファがある場合はデフォルト値としてプロンプトに表示し、
 * 空入力時はデフォルトバッファに切り替える。
 */
public class SwitchBufferCommand implements Command {

    private final InputHistory bufferHistory;

    public SwitchBufferCommand(InputHistory bufferHistory) {
        this.bufferHistory = bufferHistory;
    }

    @Override
    public String name() {
        return "switch-to-buffer";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.frame().getActiveWindow();
        var defaultName = window.getPreviousBuffer().map(b -> b.getName()).orElse("");
        var promptMessage =
                defaultName.isEmpty() ? "Switch to buffer: " : "Switch to buffer (default " + defaultName + "): ";

        var completer = new BufferNameCompleter(context.bufferManager());
        return context.inputPrompter()
                .prompt(promptMessage, "", bufferHistory, completer)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        var input = confirmed.value();
                        var bufferName = input.isEmpty() ? defaultName : input;
                        switchBuffer(context, bufferName);
                    }
                });
    }

    private void switchBuffer(CommandContext context, String bufferName) {
        if (bufferName.isEmpty()) {
            return;
        }
        var existing = context.bufferManager().findByName(bufferName);
        if (existing.isPresent()) {
            context.frame().getActiveWindow().setBuffer(existing.get());
        } else {
            var newBuffer = new EditableBuffer(bufferName, new GapTextModel());
            context.bufferManager().add(newBuffer);
            context.frame().getActiveWindow().setBuffer(newBuffer);
            context.messageBuffer().message("Buffer created: " + bufferName);
        }
    }
}
