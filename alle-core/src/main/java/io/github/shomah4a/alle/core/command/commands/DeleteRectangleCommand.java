package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * mark〜point で決まる矩形範囲を削除するコマンド（保存しない）。
 * Emacs の delete-rectangle (C-x r d) に相当する。
 */
public class DeleteRectangleCommand implements Command {

    @Override
    public String name() {
        return "delete-rectangle";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
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
        RectangleGeometry.deleteRectangle(buffer, rect, tabWidth);
        window.setPoint(RectangleGeometry.topLeftOffset(buffer, rect, tabWidth));
        buffer.markDirty();
        return CompletableFuture.completedFuture(null);
    }
}
