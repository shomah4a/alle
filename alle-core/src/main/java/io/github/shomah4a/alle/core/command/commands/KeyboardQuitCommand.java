package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;

/**
 * マークを解除しエコーエリアに "Quit" を表示するコマンド。
 * EmacsのC-g (keyboard-quit) に相当する。
 */
public class KeyboardQuitCommand implements Command {

    @Override
    public String name() {
        return "keyboard-quit";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        window.clearMark();
        context.messageBuffer().message("Quit");
        return CompletableFuture.completedFuture(null);
    }
}
