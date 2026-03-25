package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * カーソル行のコメントをトグルするコマンド。
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
        int point = window.getPoint();
        String commentString = buffer.getSettings().get(EditorSettings.COMMENT_STRING);
        int commentLen = commentString.length();

        int lineIndex = buffer.lineIndexForOffset(point);
        int lineStart = buffer.lineStartOffset(lineIndex);
        String lineText = buffer.lineText(lineIndex);

        // 行頭の空白の長さを計算
        int indentLen = 0;
        for (int i = 0; i < lineText.length(); i++) {
            if (lineText.charAt(i) == ' ' || lineText.charAt(i) == '\t') {
                indentLen++;
            } else {
                break;
            }
        }
        int contentStart = lineStart + indentLen;
        String afterIndent = lineText.substring(indentLen);

        if (afterIndent.startsWith(commentString)) {
            // コメント解除
            buffer.deleteText(contentStart, commentLen);
            window.setPoint(Math.max(point - commentLen, lineStart));
        } else if (!afterIndent.isEmpty() && afterIndent.charAt(0) == commentString.charAt(0)) {
            // コメント文字列の先頭文字のみの場合（例: "#" without space）
            buffer.deleteText(contentStart, 1);
            window.setPoint(Math.max(point - 1, lineStart));
        } else {
            // コメント挿入
            buffer.insertText(contentStart, commentString);
            window.setPoint(point + commentLen);
        }
        return CompletableFuture.completedFuture(null);
    }
}
