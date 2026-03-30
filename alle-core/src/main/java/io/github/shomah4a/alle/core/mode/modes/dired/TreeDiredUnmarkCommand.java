package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * エントリのマークを解除する。
 * C-SPCでリージョンが選択されている場合は範囲内のエントリのマークを解除し、
 * そうでない場合はカーソル行のエントリのマークを解除して次行に移動する。
 */
public class TreeDiredUnmarkCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-unmark";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var mode = window.getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        var model = diredMode.getModel();
        var regionStart = window.getRegionStart();
        var regionEnd = window.getRegionEnd();

        if (regionStart.isPresent() && regionEnd.isPresent()) {
            var entries = TreeDiredEntryResolver.resolveRange(window, diredMode, regionStart.get(), regionEnd.get());
            for (var entry : entries) {
                model.unmark(entry.path());
            }
            window.clearMark();
            TreeDiredBufferUpdater.update(window, diredMode);
            return CompletableFuture.completedFuture(null);
        } else {
            var entryOpt = TreeDiredEntryResolver.resolve(window, diredMode);
            if (entryOpt.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            model.unmark(entryOpt.get().path());
            TreeDiredBufferUpdater.update(window, diredMode);
            return context.delegate("next-line");
        }
    }
}
