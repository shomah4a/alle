package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * カーソルから行末まで削除するコマンド。
 * 行末にいる場合は改行文字を削除して次の行と結合する。
 * バッファ末尾では何もしない。
 * 削除したテキストはkill-ringに蓄積される。
 * 連続実行時は前回のエントリに追記する。
 * Emacsのkill-lineに相当する。
 */
public class KillLineCommand implements Command {

    @Override
    public String name() {
        return "kill-line";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.activeWindowActor().killLine().thenAccept(textOpt -> {
            if (textOpt.isEmpty()) {
                return;
            }
            String killedText = textOpt.get();
            boolean isConsecutiveKill =
                    context.lastCommand().map(last -> last.equals(name())).orElse(false);
            if (isConsecutiveKill) {
                context.killRing().appendToLast(killedText);
            } else {
                context.killRing().push(killedText);
            }
        });
    }
}
