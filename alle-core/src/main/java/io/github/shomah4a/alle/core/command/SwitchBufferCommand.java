package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.input.PromptResult;
import java.util.concurrent.CompletableFuture;

/**
 * バッファを切り替えるコマンド。
 * ミニバッファでバッファ名を入力させ、該当バッファに切り替える。
 */
public class SwitchBufferCommand implements Command {

    @Override
    public String name() {
        return "switch-to-buffer";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.inputPrompter().prompt("Switch to buffer: ").thenAccept(result -> {
            if (result instanceof PromptResult.Confirmed confirmed) {
                switchBuffer(context, confirmed.value());
            }
        });
    }

    private void switchBuffer(CommandContext context, String bufferName) {
        if (bufferName.isEmpty()) {
            return;
        }
        var buffer = context.bufferManager().findByName(bufferName);
        buffer.ifPresent(b -> context.frame().getActiveWindow().setBuffer(b));
    }
}
