package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * occurバッファのカーソル行に対応するソースバッファの行にジャンプし、
 * フォーカスをソースウィンドウに移すコマンド。
 */
public class OccurGotoLineCommand implements Command {

    @Override
    public String name() {
        return "occur-goto-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof OccurMode occurMode)) {
            return CompletableFuture.completedFuture(null);
        }

        var matchOpt = OccurEntryResolver.resolve(context.activeWindow(), occurMode);
        if (matchOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        String sourceBufferName = occurMode.getModel().getSourceBufferName();
        Optional<Window> sourceWindowOpt = OccurWindowHelper.findSourceWindow(context, sourceBufferName);
        if (sourceWindowOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Window sourceWindow = sourceWindowOpt.get();
        OccurMatch match = matchOpt.get();
        var sourceBuffer = sourceWindow.getBuffer();
        int lineIndex = match.lineIndex();
        if (lineIndex < sourceBuffer.lineCount()) {
            int lineStart = sourceBuffer.lineStartOffset(lineIndex);
            sourceWindow.setPoint(lineStart + match.matchOffsetInLine());
        }

        context.frame().setActiveWindow(sourceWindow);
        return CompletableFuture.completedFuture(null);
    }
}
