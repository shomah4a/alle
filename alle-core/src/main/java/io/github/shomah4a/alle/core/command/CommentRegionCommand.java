package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * 選択範囲の各行をコメントアウトするコマンド。
 * 全行がコメント済みなら解除する。
 * コメント文字列はバッファの設定（COMMENT_STRING）から取得する。
 */
public class CommentRegionCommand implements Command {

    @Override
    public String name() {
        return "comment-region";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
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

        boolean shouldUncomment = isAllCommented(buffer, startLine, endLine, commentString);

        if (shouldUncomment) {
            uncommentRegion(buffer, startLine, endLine, commentString);
        } else {
            commentRegion(buffer, startLine, endLine, commentString);
        }
        buffer.markDirty();
        return CompletableFuture.completedFuture(null);
    }

    private void commentRegion(BufferFacade buffer, int startLine, int endLine, String commentString) {
        for (int li = endLine; li >= startLine; li--) {
            int lineStart = buffer.lineStartOffset(li);
            String lineText = buffer.lineText(li);
            int indentLen = countLeadingWhitespace(lineText);
            buffer.insertText(lineStart + indentLen, commentString);
        }
    }

    private void uncommentRegion(BufferFacade buffer, int startLine, int endLine, String commentString) {
        for (int li = endLine; li >= startLine; li--) {
            String lineText = buffer.lineText(li);
            int indentLen = countLeadingWhitespace(lineText);
            int lineStart = buffer.lineStartOffset(li);
            int contentStart = lineStart + indentLen;
            String afterIndent = lineText.substring(indentLen);
            if (afterIndent.startsWith(commentString)) {
                buffer.deleteText(contentStart, commentString.length());
            } else if (!afterIndent.isEmpty() && afterIndent.charAt(0) == commentString.charAt(0)) {
                buffer.deleteText(contentStart, 1);
            }
        }
    }

    private static boolean isAllCommented(BufferFacade buffer, int startLine, int endLine, String commentString) {
        for (int li = startLine; li <= endLine; li++) {
            String lineText = buffer.lineText(li);
            String stripped = lineText.stripLeading();
            if (!stripped.isEmpty() && !stripped.startsWith(String.valueOf(commentString.charAt(0)))) {
                return false;
            }
        }
        return true;
    }

    private static int countLeadingWhitespace(String text) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ' ' || text.charAt(i) == '\t') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
}
