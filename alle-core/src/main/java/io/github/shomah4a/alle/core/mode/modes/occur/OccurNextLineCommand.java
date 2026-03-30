package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * occurバッファで次の行に移動し、ソースウィンドウの該当行にジャンプする。
 */
public class OccurNextLineCommand implements Command {

    private final Command nextLineCommand;

    public OccurNextLineCommand(Command nextLineCommand) {
        this.nextLineCommand = nextLineCommand;
    }

    @Override
    public String name() {
        return "occur-next-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return nextLineCommand.execute(context).thenRun(() -> {
            var mode = context.activeWindow().getBuffer().getMajorMode();
            if (mode instanceof OccurMode occurMode) {
                OccurEntryResolver.resolve(context.activeWindow(), occurMode)
                        .ifPresent(match -> OccurWindowHelper.jumpToSourceLine(
                                context, match, occurMode.getModel().getSourceBufferName()));
            }
        });
    }
}
