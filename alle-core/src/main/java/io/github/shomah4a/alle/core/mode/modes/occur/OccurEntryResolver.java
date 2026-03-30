package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import org.eclipse.collections.api.list.ListIterable;

/**
 * カーソル位置から対応する OccurMatch を解決する。
 */
final class OccurEntryResolver {

    private OccurEntryResolver() {}

    private static final int HEADER_LINES = 1;

    /**
     * カーソル行に対応するマッチエントリを返す。
     * ヘッダ行（0行目）の場合は empty を返す。
     */
    static Optional<OccurMatch> resolve(Window window, OccurMode mode) {
        BufferFacade buffer = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buffer.lineIndexForOffset(point);

        if (lineIndex < HEADER_LINES) {
            return Optional.empty();
        }

        ListIterable<OccurMatch> matches = mode.getModel().getMatches();
        int matchIndex = lineIndex - HEADER_LINES;
        if (matchIndex < 0 || matchIndex >= matches.size()) {
            return Optional.empty();
        }
        return Optional.of(matches.get(matchIndex));
    }
}
