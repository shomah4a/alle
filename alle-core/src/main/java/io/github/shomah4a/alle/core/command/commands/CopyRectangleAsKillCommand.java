package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.RectangleKillRing;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * mark〜point で決まる矩形範囲を RectangleKillRing にコピーするコマンド（削除しない）。
 * Emacs の copy-rectangle-as-kill (C-x r M-w) に相当する。
 */
public class CopyRectangleAsKillCommand implements Command {

    private final RectangleKillRing rectangleKillRing;

    public CopyRectangleAsKillCommand(RectangleKillRing rectangleKillRing) {
        this.rectangleKillRing = rectangleKillRing;
    }

    @Override
    public String name() {
        return "copy-rectangle-as-kill";
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
        var lines = RectangleGeometry.extractRectangle(buffer, rect, tabWidth);
        rectangleKillRing.put(lines);
        return CompletableFuture.completedFuture(null);
    }
}
