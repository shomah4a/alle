package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.util.StringMatching;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * occurの検索結果モデル。
 * 検索元バッファ名、クエリ、マッチ結果を保持する。
 */
public class OccurModel {

    private final String sourceBufferName;
    private final String query;
    private final MutableList<OccurMatch> matches;

    private OccurModel(String sourceBufferName, String query, MutableList<OccurMatch> matches) {
        this.sourceBufferName = sourceBufferName;
        this.query = query;
        this.matches = matches;
    }

    /**
     * 指定バッファ内を検索し、OccurModelを生成する。
     * 1行に複数マッチがあっても行は1回のみ記録する（最初のマッチのオフセット）。
     *
     * <p>smart-case (emacs 風): クエリに大文字を含まない場合は大文字小文字を区別せず、
     * 含む場合は区別する。判定は {@link StringMatching#containsUpperCase} を用いる。
     *
     * @param sourceBuffer 検索対象バッファ
     * @param query 検索クエリ
     * @return 検索結果のモデル
     */
    public static OccurModel search(BufferFacade sourceBuffer, String query) {
        MutableList<OccurMatch> matches = Lists.mutable.empty();
        if (query.isEmpty()) {
            return new OccurModel(sourceBuffer.getName(), query, matches);
        }

        boolean ignoreCase = !StringMatching.containsUpperCase(query);
        int lineCount = sourceBuffer.lineCount();
        for (int i = 0; i < lineCount; i++) {
            String lineText = sourceBuffer.lineText(i);
            int charIndex = StringMatching.indexOf(lineText, query, 0, ignoreCase);
            if (charIndex >= 0) {
                int codePointOffset = lineText.codePointCount(0, charIndex);
                matches.add(new OccurMatch(i, lineText, codePointOffset));
            }
        }
        return new OccurModel(sourceBuffer.getName(), query, matches);
    }

    public String getSourceBufferName() {
        return sourceBufferName;
    }

    public String getQuery() {
        return query;
    }

    public ListIterable<OccurMatch> getMatches() {
        return matches;
    }

    public int matchCount() {
        return matches.size();
    }
}
