package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * occurバッファでのself-insertを無効化するコマンド。
 */
public class OccurNoOpCommand implements Command {

    @Override
    public String name() {
        return "occur-no-op";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return CompletableFuture.completedFuture(null);
    }
}
