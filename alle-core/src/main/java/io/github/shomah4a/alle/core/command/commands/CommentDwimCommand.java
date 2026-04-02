package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * カーソル行のコメントをトグルするコマンド。
 * リージョンがアクティブな場合はリージョン全体のコメントをトグルする。
 * コメント文字列はバッファの設定（COMMENT_STRING）から取得する。
 * 行がコメント行ならコメント文字列を削除し、そうでなければ挿入する。
 */
public class CommentDwimCommand implements Command {

    @Override
    public String name() {
        return "comment-dwim";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();

        // リージョンがアクティブならリージョン全体をトグル
        var regionStart = window.getRegionStart();
        var regionEnd = window.getRegionEnd();
        if (regionStart.isPresent() && regionEnd.isPresent()) {
            String commentString = buffer.getSettings().get(EditorSettings.COMMENT_STRING);
            int startLine = buffer.lineIndexForOffset(regionStart.get());
            int endLine = buffer.lineIndexForOffset(regionEnd.get());
            if (CommentRegionUtil.isAllCommented(buffer, startLine, endLine, commentString)) {
                CommentRegionUtil.uncommentRegion(buffer, startLine, endLine, commentString);
            } else {
                CommentRegionUtil.commentRegion(buffer, startLine, endLine, commentString);
            }
            buffer.markDirty();
            return CompletableFuture.completedFuture(null);
        }

        // 単一行のトグル
        int point = window.getPoint();
        String commentString = buffer.getSettings().get(EditorSettings.COMMENT_STRING);
        int commentLen = commentString.length();

        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        String lineText = buffer.lineText(lineIndex);

        int indentLen = CommentRegionUtil.countLeadingWhitespace(lineText);
        int contentStart = lineStart + indentLen;
        String afterIndent = lineText.substring(indentLen);

        if (afterIndent.startsWith(commentString)) {
            buffer.deleteText(contentStart, commentLen);
            window.setPoint(Math.max(point - commentLen, lineStart));
        } else if (!afterIndent.isEmpty() && afterIndent.charAt(0) == commentString.charAt(0)) {
            buffer.deleteText(contentStart, 1);
            window.setPoint(Math.max(point - 1, lineStart));
        } else {
            buffer.insertText(contentStart, commentString);
            window.setPoint(point + commentLen);
        }
        return CompletableFuture.completedFuture(null);
    }
}
