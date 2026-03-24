package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.window.Direction;
import java.util.concurrent.CompletableFuture;

/**
 * アクティブウィンドウを上下に分割するコマンド。
 * 新しいウィンドウは同一バッファを表示し、カーソルは元のウィンドウに留まる。
 */
public class SplitWindowBelowCommand implements Command {

    @Override
    public String name() {
        return "split-window-below";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.frameActor().isMinibufferActive().thenCompose(active -> {
            if (active) {
                return CompletableFuture.completedFuture(null);
            }
            return context.frameActor().splitActiveWindowKeepFocus(Direction.HORIZONTAL);
        });
    }
}
