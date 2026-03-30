package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.window.Direction;
import java.util.concurrent.CompletableFuture;

/**
 * アクティブウィンドウを左右に分割するコマンド。
 * 新しいウィンドウは同一バッファを表示し、カーソルは元のウィンドウに留まる。
 */
public class SplitWindowRightCommand implements Command {

    @Override
    public String name() {
        return "split-window-right";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var frame = context.frame();
        if (frame.isMinibufferActive()) {
            return CompletableFuture.completedFuture(null);
        }
        var originalWindow = frame.getActiveWindow();
        var buffer = originalWindow.getBuffer();
        frame.splitActiveWindow(Direction.VERTICAL, buffer);
        frame.setActiveWindow(originalWindow);
        return CompletableFuture.completedFuture(null);
    }
}
