package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import org.eclipse.collections.api.list.ListIterable;

/**
 * カーソル位置から対応する TreeDiredEntry を解決する。
 */
final class TreeDiredEntryResolver {

    private TreeDiredEntryResolver() {}

    private static final int HEADER_LINES = 2; // ルートパス行 + カラムヘッダ行

    /**
     * カーソル行に対応するエントリを返す。
     * ヘッダ行（0-1行目）の場合は empty を返す。
     */
    static Optional<TreeDiredEntry> resolve(Window window, TreeDiredMode mode) {
        BufferFacade buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);

        // 0-1行目はヘッダ行
        if (lineIndex < HEADER_LINES) {
            return Optional.empty();
        }

        ListIterable<TreeDiredEntry> entries = mode.getModel().getVisibleEntries();
        int entryIndex = lineIndex - HEADER_LINES;
        if (entryIndex < 0 || entryIndex >= entries.size()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(entryIndex));
    }
}
