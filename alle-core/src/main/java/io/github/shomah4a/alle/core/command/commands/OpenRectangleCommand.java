package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.TransactionalCommand;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * mark〜point で決まる矩形範囲と同サイズの空白を挿入するコマンド。
 * 矩形右側のテキストは右に押し出される。
 * Emacs の open-rectangle (C-x r o) に相当する。
 */
public class OpenRectangleCommand implements TransactionalCommand {

    @Override
    public String name() {
        return "open-rectangle";
    }

    @Override
    public CompletableFuture<Void> executeInTransaction(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        int tabWidth = buffer.getSettings().get(EditorSettings.TAB_WIDTH);
        var rectOpt = RectangleGeometry.fromRegion(window, tabWidth);
        if (rectOpt.isEmpty()) {
            context.messageBuffer().message("No region active");
            return CompletableFuture.completedFuture(null);
        }
        var rect = rectOpt.get();
        if (rect.width() == 0) {
            return CompletableFuture.completedFuture(null);
        }
        RectangleGeometry.openRectangle(buffer, rect, tabWidth);
        buffer.markDirty();
        return CompletableFuture.completedFuture(null);
    }
}
