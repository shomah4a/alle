package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
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
        var frame = context.frame();
        if (frame.isMinibufferActive()) {
            return CompletableFuture.completedFuture(null);
        }
        var originalWindow = frame.getActiveWindow();
        var buffer = originalWindow.getBuffer();
        frame.splitActiveWindow(Direction.HORIZONTAL, buffer);
        // splitActiveWindowはアクティブを新ウィンドウに切り替えるので元に戻す
        frame.setActiveWindow(originalWindow);
        return CompletableFuture.completedFuture(null);
    }
}
