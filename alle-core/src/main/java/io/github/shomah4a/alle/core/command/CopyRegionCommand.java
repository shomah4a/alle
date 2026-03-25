package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * mark〜point間のテキストをkill-ringにコピーするコマンド（削除しない）。
 * Emacsのkill-ring-save (M-w) に相当する。
 * markが未設定の場合は何もしない。
 */
public class CopyRegionCommand implements Command {

    @Override
    public String name() {
        return "copy-region";
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
        String copiedText = buffer.substring(start, end);
        window.clearMark();

        context.killRing().push(copiedText);
        return CompletableFuture.completedFuture(null);
    }
}
