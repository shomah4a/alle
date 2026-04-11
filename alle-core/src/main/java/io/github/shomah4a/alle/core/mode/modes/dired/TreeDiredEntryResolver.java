package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.window.Window;
import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * カーソル位置から対応する TreeDiredEntry を解決する。
 */
public final class TreeDiredEntryResolver {

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

    /**
     * 指定されたオフセット範囲（リージョン）に含まれる行のエントリをすべて返す。
     */
    static ListIterable<TreeDiredEntry> resolveRange(
            Window window, TreeDiredMode mode, int regionStart, int regionEnd) {
        BufferFacade buffer = window.getBuffer();
        int startLine = buffer.lineIndexForOffset(regionStart);
        int endLine = buffer.lineIndexForOffset(regionEnd);

        ListIterable<TreeDiredEntry> entries = mode.getModel().getVisibleEntries();
        MutableList<TreeDiredEntry> result = Lists.mutable.empty();

        for (int line = startLine; line <= endLine; line++) {
            int entryIndex = line - HEADER_LINES;
            if (entryIndex >= 0 && entryIndex < entries.size()) {
                result.add(entries.get(entryIndex));
            }
        }
        return result;
    }

    /**
     * 対象エントリの親ディレクトリがすべて同一かどうかを返す。
     * 対象が1件以下の場合は常に true を返す。
     */
    public static boolean hasSameParentDirectory(ListIterable<TreeDiredEntry> targets) {
        if (targets.size() <= 1) {
            return true;
        }
        Path firstParent = targets.get(0).path().getParent();
        return targets.allSatisfy(e -> {
            Path parent = e.path().getParent();
            if (firstParent == null) {
                return parent == null;
            }
            return firstParent.equals(parent);
        });
    }

    /**
     * マーク済みエントリがあればそれを返し、なければカーソル行のエントリを返す。
     * ファイル操作コマンドの対象解決用。
     */
    public static ListIterable<TreeDiredEntry> resolveTargets(Window window, TreeDiredMode mode) {
        var markedPaths = mode.getModel().getMarkedPaths();
        if (markedPaths.notEmpty()) {
            ListIterable<TreeDiredEntry> entries = mode.getModel().getVisibleEntries();
            return entries.select(e -> markedPaths.contains(e.path()));
        }
        return resolve(window, mode).map(Lists.immutable::of).orElseGet(Lists.immutable::empty);
    }
}
