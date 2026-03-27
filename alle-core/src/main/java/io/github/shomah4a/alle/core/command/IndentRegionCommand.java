package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * 選択範囲の各行をインデント1レベル増加するコマンド。
 */
public class IndentRegionCommand implements Command {

    @Override
    public String name() {
        return "indent-region";
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

        int indentWidth = buffer.getSettings().get(EditorSettings.INDENT_WIDTH);
        String indentStr = " ".repeat(indentWidth);
        int startLine = buffer.lineIndexForOffset(regionStart.get());
        int endLine = buffer.lineIndexForOffset(regionEnd.get());

        // 末尾行から処理してオフセットのずれを防ぐ
        buffer.getUndoManager().withTransaction(() -> {
            for (int li = endLine; li >= startLine; li--) {
                int lineStart = buffer.lineStartOffset(li);
                buffer.insertText(lineStart, indentStr);
            }
        });
        buffer.markDirty();
        return CompletableFuture.completedFuture(null);
    }
}
