package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;

/**
 * 選択範囲の各行をインデント1レベル減少するコマンド。
 */
public class DedentRegionCommand implements Command {

    @Override
    public String name() {
        return "dedent-region";
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
        int startLine = buffer.lineIndexForOffset(regionStart.get());
        int endLine = buffer.lineIndexForOffset(regionEnd.get());

        buffer.getUndoManager().withTransaction(() -> {
            for (int li = endLine; li >= startLine; li--) {
                int lineStart = buffer.lineStartOffset(li);
                String lineText = buffer.lineText(li);
                int spaceCount = 0;
                for (int i = 0; i < lineText.length() && i < indentWidth; i++) {
                    if (lineText.charAt(i) == ' ') {
                        spaceCount++;
                    } else {
                        break;
                    }
                }
                if (spaceCount > 0) {
                    buffer.deleteText(lineStart, spaceCount);
                }
            }
        });
        buffer.markDirty();
        return CompletableFuture.completedFuture(null);
    }
}
