package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * バッファ全体を選択するコマンド。
 * Emacsのmark-whole-buffer (C-x h) に相当する。
 * pointをバッファ先頭に移動し、markをバッファ末尾に設定する。
 */
public class MarkWholeBufferCommand implements Command {

    @Override
    public String name() {
        return "mark-whole-buffer";
    }

    @Override
    public boolean keepsRegionActive() {
        return true;
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        window.setMark(buffer.length());
        window.setPoint(0);
        return CompletableFuture.completedFuture(null);
    }
}
