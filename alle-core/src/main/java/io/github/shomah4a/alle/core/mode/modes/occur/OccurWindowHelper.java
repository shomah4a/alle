package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;

/**
 * occurバッファのソースバッファを表示しているウィンドウを探索するヘルパー。
 */
final class OccurWindowHelper {

    private OccurWindowHelper() {}

    /**
     * ソースバッファを表示しているウィンドウを探す。
     * 複数ウィンドウが同一バッファを表示している場合は最初に見つかったものを返す。
     * アクティブウィンドウ（occurウィンドウ自体）は除外する。
     */
    static Optional<Window> findSourceWindow(CommandContext context, String sourceBufferName) {
        Optional<BufferFacade> sourceBuffer = context.bufferManager().findByName(sourceBufferName);
        if (sourceBuffer.isEmpty()) {
            return Optional.empty();
        }

        var windows = context.frame().getWindowTree().windows();
        Window activeWindow = context.activeWindow();
        for (Window window : windows) {
            if (window != activeWindow && window.getBuffer().equals(sourceBuffer.get())) {
                return Optional.of(window);
            }
        }
        return Optional.empty();
    }

    /**
     * ソースウィンドウの該当行にジャンプする。
     * ソースウィンドウが見つからない場合は何もしない。
     */
    static void jumpToSourceLine(CommandContext context, OccurMatch match, String sourceBufferName) {
        findSourceWindow(context, sourceBufferName).ifPresent(sourceWindow -> {
            BufferFacade sourceBuffer = sourceWindow.getBuffer();
            int lineIndex = match.lineIndex();
            if (lineIndex < sourceBuffer.lineCount()) {
                int lineStart = sourceBuffer.lineStartOffset(lineIndex);
                sourceWindow.setPoint(lineStart + match.matchOffsetInLine());
            }
        });
    }
}
