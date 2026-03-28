package io.github.shomah4a.alle.core.styling;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;
import org.treesitter.TSInputEdit;
import org.treesitter.TSLanguage;
import org.treesitter.TSNode;
import org.treesitter.TSParser;
import org.treesitter.TSPoint;
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
 * <p>前回のスタイリング結果とTSTreeをキャッシュする。
 * テキストが変更されていない場合はキャッシュ結果を返し、
 * 変更がある場合はTree-sitterのインクリメンタルパースで差分のみ再解析する。
 * スタイラーのインスタンスはバッファごとのモードが保持するため、ステートのスコープはバッファに閉じている。
 */
public class TreeSitterStyler implements SyntaxStyler {

    private final TSLanguage language;
    private final String queryString;
    private final NodeFaceMapping captureMapping;

    private @Nullable String cachedText;
    private ListIterable<ListIterable<StyledSpan>> cachedResult = Lists.mutable.empty();
    private @Nullable TSTree cachedTree;

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
            clearCache();
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

        // parse()直後にcachedTreeを更新する。
        // parseIncremental内で旧treeをcloseするため、
        // 後続処理で例外が発生してもclose済みオブジェクトを参照し続けることを防ぐ。
        TSTree tree = parse(fullText);
        cachedTree = tree;
        TSNode rootNode = tree.getRootNode();

        try (TSQuery query = new TSQuery(language, queryString);
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
     * テキストをパースしてTSTreeを返す。
     * cachedTreeがある場合はインクリメンタルパースを行い、ない場合はフルパースを行う。
     * 返却されたTSTreeは呼び出し側でcachedTreeに設定すること。
     */
    private TSTree parse(String fullText) {
        try (TSParser parser = new TSParser()) {
            parser.setLanguage(language);
            if (cachedTree != null && cachedText != null) {
                return parseIncremental(parser, fullText, cachedTree, cachedText);
            }
            return parser.parseString(null, fullText);
        }
    }

    /**
     * インクリメンタルパースを実行する。
     * cachedTreeに変更箇所を通知し、差分パースで新しいツリーを生成する。
     * 失敗した場合はフルパースにフォールバックする。
     */
    private TSTree parseIncremental(TSParser parser, String fullText, TSTree oldTree, String oldText) {
        try {
            TSInputEdit edit = computeInputEdit(oldText, fullText);
            oldTree.edit(edit);
            TSTree newTree = parser.parseString(oldTree, fullText);
            oldTree.close();
            return newTree;
        } catch (RuntimeException e) {
            // フォールバック: インクリメンタルパース失敗時はフルパース
            oldTree.close();
            cachedTree = null;
            return parser.parseString(null, fullText);
        }
    }

    /**
     * 旧テキストと新テキストの差分からTSInputEditを構築する。
     *
     * <p>先頭と末尾の共通部分をスキャンして変更箇所を特定し、
     * UTF-8バイトオフセットとTSPointに変換する。
     */
    static TSInputEdit computeInputEdit(String oldText, String newText) {
        int oldLen = (int) oldText.codePoints().count();
        int newLen = (int) newText.codePoints().count();

        // 共通プレフィックス長（コードポイント単位）
        int commonPrefix = computeCommonPrefixLength(oldText, newText);

        // 共通サフィックス長（コードポイント単位）。プレフィックスと重複しないよう制限
        int commonSuffix = computeCommonSuffixLength(oldText, newText);
        commonSuffix = Math.min(commonSuffix, oldLen - commonPrefix);
        commonSuffix = Math.min(commonSuffix, newLen - commonPrefix);

        int changeStartCp = commonPrefix;
        int oldEndCp = oldLen - commonSuffix;
        int newEndCp = newLen - commonSuffix;

        int startByte = Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset(oldText, changeStartCp);
        int oldEndByte = Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset(oldText, oldEndCp);
        int newEndByte = Utf8OffsetConverter.codePointOffsetToUtf8ByteOffset(newText, newEndCp);

        Utf8Position startPos = Utf8OffsetConverter.codePointOffsetToRowColumn(oldText, changeStartCp);
        Utf8Position oldEndPos = Utf8OffsetConverter.codePointOffsetToRowColumn(oldText, oldEndCp);
        Utf8Position newEndPos = Utf8OffsetConverter.codePointOffsetToRowColumn(newText, newEndCp);

        return new TSInputEdit(
                startByte,
                oldEndByte,
                newEndByte,
                new TSPoint(startPos.row(), startPos.column()),
                new TSPoint(oldEndPos.row(), oldEndPos.column()),
                new TSPoint(newEndPos.row(), newEndPos.column()));
    }

    /**
     * 2つのテキストの共通プレフィックス長をコードポイント単位で返す。
     */
    private static int computeCommonPrefixLength(String a, String b) {
        int count = 0;
        int ai = 0;
        int bi = 0;
        while (ai < a.length() && bi < b.length()) {
            int cpA = a.codePointAt(ai);
            int cpB = b.codePointAt(bi);
            if (cpA != cpB) {
                break;
            }
            count++;
            ai += Character.charCount(cpA);
            bi += Character.charCount(cpB);
        }
        return count;
    }

    /**
     * 2つのテキストの共通サフィックス長をコードポイント単位で返す。
     */
    private static int computeCommonSuffixLength(String a, String b) {
        int count = 0;
        int ai = a.length();
        int bi = b.length();
        while (ai > 0 && bi > 0) {
            int cpA = Character.codePointBefore(a, ai);
            int cpB = Character.codePointBefore(b, bi);
            if (cpA != cpB) {
                break;
            }
            count++;
            ai -= Character.charCount(cpA);
            bi -= Character.charCount(cpB);
        }
        return count;
    }

    private void clearCache() {
        closeCachedTree();
        cachedText = null;
        cachedResult = Lists.mutable.empty();
    }

    private void closeCachedTree() {
        if (cachedTree != null) {
            cachedTree.close();
            cachedTree = null;
        }
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
