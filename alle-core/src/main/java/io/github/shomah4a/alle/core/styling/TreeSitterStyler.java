package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;
import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
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
 * <p>前回のスタイリング結果をキャッシュし、テキストが変更されていない場合は
 * パースをスキップしてキャッシュ結果を返す。
 * スタイラーのインスタンスはバッファごとのモードが保持するため、ステートのスコープはバッファに閉じている。
 */
public class TreeSitterStyler implements SyntaxStyler {

    private final TSLanguage language;
    private final String queryString;
    private final NodeFaceMapping captureMapping;

    private @Nullable String cachedText;
    private ListIterable<ListIterable<StyledSpan>> cachedResult = Lists.mutable.empty();

    /**
     * @param language Tree-sitterの言語定義
     * @param queryString S式クエリ文字列（キャプチャ名でノードを分類）
     * @param captureMapping キャプチャ名からFaceNameへのマッピング
     */
    public TreeSitterStyler(TSLanguage language, String queryString, NodeFaceMapping captureMapping) {
        this.language = language;
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
            return Lists.mutable.empty();
        }

        String fullText = String.join("\n", lines.toArray(new String[0]));

        if (fullText.equals(cachedText)) {
            return cachedResult;
        }

        MutableList<MutableList<StyledSpan>> result = Lists.mutable.withInitialCapacity(lineCount);
        for (int i = 0; i < lineCount; i++) {
            result.add(Lists.mutable.empty());
        }

        TSParser parser = new TSParser();
        parser.setLanguage(language);
        TSTree tree = parser.parseString(null, fullText);
        TSNode rootNode = tree.getRootNode();

        TSQuery query = new TSQuery(language, queryString);
        TSQueryCursor cursor = new TSQueryCursor();
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

        cursor.close();
        query.close();
        tree.close();
        parser.close();

        // 各行のスパンをstart順にソート
        MutableList<ListIterable<StyledSpan>> sorted = Lists.mutable.withInitialCapacity(lineCount);
        for (MutableList<StyledSpan> lineSpans : result) {
            lineSpans.sortThis((a, b) -> Integer.compare(a.start(), b.start()));
            sorted.add(lineSpans);
        }

        cachedText = fullText;
        cachedResult = sorted;

        return sorted;
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
