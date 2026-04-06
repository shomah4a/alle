package io.github.shomah4a.alle.core.styling;

import io.github.shomah4a.alle.core.syntax.TreeSitterSession;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;
import org.treesitter.TSNode;
import org.treesitter.TSQuery;
import org.treesitter.TSQueryCapture;
import org.treesitter.TSQueryCursor;
import org.treesitter.TSQueryMatch;
import org.treesitter.TSTree;

/**
 * Tree-sitterパーサーとクエリを使用したシンタックススタイラー。
 *
 * <p>ドキュメント全体をパースし、S式クエリのキャプチャに基づいてスタイリングを行う。
 * {@link #styleDocument(ListIterable)} をオーバーライドし、
 * 行単位の {@link #styleLine(String)} は単一行ドキュメントとして処理する。
 *
 * <p>パースは{@link TreeSitterSession}に委譲し、同一セッションを共有する
 * TreeSitterAnalyzerと2重パースを回避する。
 * 前回のスタイリング結果をキャッシュし、テキストが変更されていない場合はキャッシュを返す。
 */
public class TreeSitterStyler implements SyntaxStyler {

    private final TreeSitterSession session;
    private final String queryString;
    private final NodeFaceMapping captureMapping;

    private @Nullable String cachedText;
    private ListIterable<ListIterable<StyledSpan>> cachedResult = Lists.mutable.empty();

    /**
     * @param session パースを管理するセッション
     * @param queryString S式クエリ文字列（キャプチャ名でノードを分類）
     * @param captureMapping キャプチャ名からFaceNameへのマッピング
     */
    public TreeSitterStyler(TreeSitterSession session, String queryString, NodeFaceMapping captureMapping) {
        this.session = session;
        this.queryString = queryString;
        this.captureMapping = captureMapping;
    }

    @Override
    public ListIterable<StyledSpan> styleLine(String lineText) {
        return styleDocument(Lists.immutable.of(lineText)).get(0);
    }

    @Override
    public ListIterable<ListIterable<StyledSpan>> styleDocument(ListIterable<String> lines) {
        int lineCount = lines.size();
        if (lineCount == 0) {
            cachedText = null;
            cachedResult = Lists.mutable.empty();
            return cachedResult;
        }

        String fullText = lines.makeString("\n");

        if (fullText.equals(cachedText)) {
            return cachedResult;
        }

        MutableList<MutableList<StyledSpan>> result = Lists.mutable.withInitialCapacity(lineCount);
        for (int i = 0; i < lineCount; i++) {
            result.add(Lists.mutable.empty());
        }

        TSTree tree = session.parse(lines);
        TSNode rootNode = tree.getRootNode();

        try (TSQuery query = new TSQuery(session.language(), queryString);
                TSQueryCursor cursor = new TSQueryCursor()) {
            cursor.exec(query, rootNode);

            TSQueryMatch match = new TSQueryMatch();
            while (cursor.nextMatch(match)) {
                for (TSQueryCapture capture : match.getCaptures()) {
                    String captureName = query.getCaptureNameForId(capture.getIndex());
                    captureMapping.resolve(captureName).ifPresent(faceName -> {
                        addNodeSpans(capture.getNode(), lines, result, faceName);
                    });
                }
            }
        }

        // 各行のスパンをstart順にソートし、同一範囲の重複を解決する。
        // TSQueryCursor.nextMatch()は同一ノードに複数パターンがマッチした場合、
        // より具体的なパターンのマッチを先に返す。List.sort()は安定ソートのため、
        // 同一startのスパンは追加順を維持する。
        // 同一[start, end)のスパンが複数存在する場合、最初のもの（最も具体的なパターン）を残す。
        MutableList<ListIterable<StyledSpan>> sorted = Lists.mutable.withInitialCapacity(lineCount);
        for (MutableList<StyledSpan> lineSpans : result) {
            lineSpans.sortThis((a, b) -> Integer.compare(a.start(), b.start()));
            sorted.add(deduplicateSpans(lineSpans));
        }

        cachedText = fullText;
        cachedResult = sorted;

        return sorted;
    }

    /**
     * 同一範囲[start, end)のスパンが複数存在する場合、最初のもののみを残す。
     * start順にソート済みのリストを前提とする。
     *
     * <p>tree-sitterのクエリマッチでは、より具体的なパターン（親ノードの条件を含む等）の
     * マッチが先に返される。例えばYAMLの{@code string_scalar}ノードに対して、
     * {@code (block_mapping_pair key: ... (string_scalar) @property)} のような
     * 具体的パターンが汎用の {@code (string_scalar) @string} より先にマッチする。
     * したがって同一範囲の最初のスパンが最も具体的なキャプチャに対応する。
     *
     * <p>このメソッドに渡されるスパンは{@link NodeFaceMapping#resolve(String)}で
     * FaceNameに解決済みのもののみである。マッピングできないキャプチャは
     * {@link #styleDocument(ListIterable)}でスパン生成自体がスキップされるため、
     * 常にマッピング可能なスパンが選択される。
     */
    private static ListIterable<StyledSpan> deduplicateSpans(MutableList<StyledSpan> spans) {
        if (spans.size() <= 1) {
            return spans;
        }
        MutableList<StyledSpan> deduplicated = Lists.mutable.withInitialCapacity(spans.size());
        for (int i = 0; i < spans.size(); i++) {
            StyledSpan current = spans.get(i);
            // 前のスパンが同一範囲なら、現在のスパンをスキップする
            if (i > 0) {
                StyledSpan prev = spans.get(i - 1);
                if (current.start() == prev.start() && current.end() == prev.end()) {
                    continue;
                }
            }
            deduplicated.add(current);
        }
        return deduplicated;
    }

    /**
     * ノードの範囲をStyledSpanとして各行に追加する。
     * ノードが複数行にまたがる場合は行ごとに分割する。
     */
    private void addNodeSpans(
            TSNode node, ListIterable<String> lines, MutableList<MutableList<StyledSpan>> result, FaceName faceName) {
        int startRow = node.getStartPoint().getRow();
        int startCol = node.getStartPoint().getColumn();
        int endRow = node.getEndPoint().getRow();
        int endCol = node.getEndPoint().getColumn();

        if (startRow >= lines.size()) {
            return;
        }

        if (startRow == endRow) {
            String lineText = lines.get(startRow);
            int cpStart = Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset(lineText, startCol);
            int cpEnd = Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset(lineText, endCol);
            if (cpStart < cpEnd) {
                result.get(startRow).add(new StyledSpan(cpStart, cpEnd, faceName));
            }
        } else {
            for (int row = startRow; row <= endRow && row < lines.size(); row++) {
                String lineText = lines.get(row);
                int lineCpLen = (int) lineText.codePoints().count();

                int cpStart;
                int cpEnd;
                if (row == startRow) {
                    cpStart = Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset(lineText, startCol);
                    cpEnd = lineCpLen;
                } else if (row == endRow) {
                    cpStart = 0;
                    cpEnd = Utf8OffsetConverter.utf8ByteOffsetToCodePointOffset(lineText, endCol);
                } else {
                    cpStart = 0;
                    cpEnd = lineCpLen;
                }

                if (cpStart < cpEnd) {
                    result.get(row).add(new StyledSpan(cpStart, cpEnd, faceName));
                }
            }
        }
    }
}
