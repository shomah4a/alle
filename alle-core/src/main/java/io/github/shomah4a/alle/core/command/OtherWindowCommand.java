package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * 次のウィンドウに切り替えるコマンド。
 * ウィンドウツリーの深さ優先順で循環する。
 */
public class OtherWindowCommand implements Command {

    @Override
    public String name() {
        return "other-window";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.frameActor().nextWindow();
    }
}
