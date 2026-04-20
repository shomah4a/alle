package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.RectangleKillRing;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * RectangleKillRing の最新矩形を point 位置に挿入するコマンド。
 * Emacs の yank-rectangle (C-x r y) に相当する。
 * 未保存の場合は何もしない。
 */
public class YankRectangleCommand implements Command {

    private final RectangleKillRing rectangleKillRing;

    public YankRectangleCommand(RectangleKillRing rectangleKillRing) {
        this.rectangleKillRing = rectangleKillRing;
    }

    @Override
    public String name() {
        return "yank-rectangle";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        var rectOpt = rectangleKillRing.current();
        if (rectOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        int tabWidth = buffer.getSettings().get(EditorSettings.TAB_WIDTH);
        int pointOffset = window.getPoint();
        RectangleGeometry.insertRectangleAtPoint(buffer, pointOffset, rectOpt.get(), tabWidth);
        buffer.markDirty();
        return CompletableFuture.completedFuture(null);
    }
}
