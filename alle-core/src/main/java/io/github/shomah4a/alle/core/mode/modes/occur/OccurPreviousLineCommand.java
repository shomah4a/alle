package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * occurバッファで前の行に移動し、ソースウィンドウの該当行にジャンプする。
 */
public class OccurPreviousLineCommand implements Command {

    private final Command previousLineCommand;

    public OccurPreviousLineCommand(Command previousLineCommand) {
        this.previousLineCommand = previousLineCommand;
    }

    @Override
    public String name() {
        return "occur-previous-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return previousLineCommand.execute(context).thenRun(() -> {
            var mode = context.activeWindow().getBuffer().getMajorMode();
            if (mode instanceof OccurMode occurMode) {
                OccurEntryResolver.resolve(context.activeWindow(), occurMode)
                        .ifPresent(match -> OccurWindowHelper.jumpToSourceLine(
                                context, match, occurMode.getModel().getSourceBufferName()));
            }
        });
    }
}
