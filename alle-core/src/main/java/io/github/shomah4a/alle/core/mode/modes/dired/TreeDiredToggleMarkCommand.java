package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * マーク状態をトグルする。
 * C-SPCでリージョンが選択されている場合は範囲内のエントリをトグルし、
 * そうでない場合はカーソル行のエントリをトグルして次行に移動する。
 */
public class TreeDiredToggleMarkCommand implements Command {

    @Override
    public String name() {
        return "tree-dired-toggle-mark";
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
                model.toggleMark(entry.path());
            }
            window.clearMark();
            TreeDiredBufferUpdater.update(window, diredMode);
            return CompletableFuture.completedFuture(null);
        } else {
            var entryOpt = TreeDiredEntryResolver.resolve(window, diredMode);
            if (entryOpt.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            model.toggleMark(entryOpt.get().path());
            TreeDiredBufferUpdater.update(window, diredMode);
            return context.delegate("next-line");
        }
    }
}
