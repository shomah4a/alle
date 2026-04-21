package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.TransactionalCommand;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * 選択範囲の各行をコメントアウトするコマンド。
 * コメント文字列はバッファの設定（COMMENT_STRING）から取得する。
 */
public class CommentRegionCommand implements TransactionalCommand {

    @Override
    public String name() {
        return "comment-region";
    }

    @Override
    public CompletableFuture<Void> executeInTransaction(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        var regionStart = window.getRegionStart();
        var regionEnd = window.getRegionEnd();
        if (regionStart.isEmpty() || regionEnd.isEmpty()) {
            context.messageBuffer().message("No region active");
            return CompletableFuture.completedFuture(null);
        }

        String commentString = buffer.getSettings().get(EditorSettings.COMMENT_STRING);
        int startLine = buffer.lineIndexForOffset(regionStart.get());
        int endLine = buffer.lineIndexForOffset(regionEnd.get());

        CommentRegionUtil.commentRegion(buffer, startLine, endLine, commentString);
        buffer.markDirty();
        return CompletableFuture.completedFuture(null);
    }
}
