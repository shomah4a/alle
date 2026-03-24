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
        return context.activeWindowActor()
                .killRegion()
                .thenAccept(textOpt ->
                        textOpt.ifPresent(killedText -> context.killRing().push(killedText)));
    }
}
