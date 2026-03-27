package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * mark〜point間のテキストを削除しkill-ringに蓄積するコマンド。
 * Emacsのkill-region (C-w) に相当する。
 * markが未設定の場合は何もしない。
 */
public class KillRegionCommand implements Command {

    @Override
    public String name() {
        return "kill-region";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var regionStart = window.getRegionStart();
        var regionEnd = window.getRegionEnd();
        if (regionStart.isEmpty() || regionEnd.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        int start = regionStart.get();
        int end = regionEnd.get();
        if (start == end) {
            return CompletableFuture.completedFuture(null);
        }

        var buffer = window.getBuffer();
        String killedText = buffer.substring(start, end);
        var inverseChange = buffer.deleteText(start, end - start);
        buffer.getUndoManager().record(inverseChange);
        buffer.markDirty();
        window.setPoint(start);
        window.clearMark();

        context.killRing().push(killedText);
        return CompletableFuture.completedFuture(null);
    }
}
