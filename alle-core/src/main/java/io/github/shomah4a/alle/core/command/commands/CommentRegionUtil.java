package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.buffer.BufferFacade;

/**
 * コメント・アンコメント操作の共通ロジック。
 */
final class CommentRegionUtil {

    private CommentRegionUtil() {}

    static void commentRegion(BufferFacade buffer, int startLine, int endLine, String commentString) {
        for (int li = endLine; li >= startLine; li--) {
            int lineStart = buffer.lineStartOffset(li);
            String lineText = buffer.lineText(li);
            int indentLen = countLeadingWhitespace(lineText);
            buffer.insertText(lineStart + indentLen, commentString);
        }
    }

    static void uncommentRegion(BufferFacade buffer, int startLine, int endLine, String commentString) {
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

    static boolean isAllCommented(BufferFacade buffer, int startLine, int endLine, String commentString) {
        for (int li = startLine; li <= endLine; li++) {
            String lineText = buffer.lineText(li);
            String stripped = lineText.stripLeading();
            if (!stripped.isEmpty() && !stripped.startsWith(String.valueOf(commentString.charAt(0)))) {
                return false;
            }
        }
        return true;
    }

    static int countLeadingWhitespace(String text) {
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
