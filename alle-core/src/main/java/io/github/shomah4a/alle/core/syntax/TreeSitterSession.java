package io.github.shomah4a.alle.core.syntax;

import io.github.shomah4a.alle.core.styling.Utf8OffsetConverter;
import io.github.shomah4a.alle.core.styling.Utf8Position;
import org.eclipse.collections.api.list.ListIterable;
import org.jspecify.annotations.Nullable;
import org.treesitter.TSInputEdit;
import org.treesitter.TSLanguage;
import org.treesitter.TSParser;
import org.treesitter.TSPoint;
import org.treesitter.TSTree;

/**
 * Tree-sitterのパースとTSTreeのキャッシュ管理を一元化するセッション。
 *
 * <p>TreeSitterStylerとTreeSitterAnalyzerが共有する。
 * テキストが変更されていない場合はキャッシュを返し、
 * 変更がある場合はインクリメンタルパースで差分のみ再解析する。
 *
 * <p>インスタンスはバッファごとのモードが保持するため、ステートのスコープはバッファに閉じている。
 */
public class TreeSitterSession {

    private final TSLanguage language;
    private @Nullable String cachedText;
    private @Nullable TSTree cachedTree;

    public TreeSitterSession(TSLanguage language) {
        this.language = language;
    }

    /**
     * このセッションが使用するTree-sitterの言語定義を返す。
     */
    public TSLanguage language() {
        return language;
    }

    /**
     * テキストをパースしてTSTreeを返す。
     * 前回と同じテキストの場合はキャッシュを返す。
     * 前回のツリーがある場合はインクリメンタルパースを行う。
     *
     * @param lines 各行のテキスト（改行文字を含まない）
     * @return パース済みのTSTree
     */
    public TSTree parse(ListIterable<String> lines) {
        String fullText = lines.isEmpty() ? "" : lines.makeString("\n");

        if (fullText.equals(cachedText) && cachedTree != null) {
            return cachedTree;
        }

        TSTree newTree = doParse(fullText);
        cachedTree = newTree;
        cachedText = fullText;
        return newTree;
    }

    /**
     * 前回のキャッシュテキストを返す。
     */
    public @Nullable String cachedText() {
        return cachedText;
    }

    private TSTree doParse(String fullText) {
        try (TSParser parser = new TSParser()) {
            parser.setLanguage(language);
            if (cachedTree != null && cachedText != null) {
                return parseIncremental(parser, fullText, cachedTree, cachedText);
            }
            return parser.parseString(null, fullText);
        }
    }

    private TSTree parseIncremental(TSParser parser, String fullText, TSTree oldTree, String oldText) {
        try {
            TSInputEdit edit = computeInputEdit(oldText, fullText);
            oldTree.edit(edit);
            TSTree newTree = parser.parseString(oldTree, fullText);
            oldTree.close();
            return newTree;
        } catch (RuntimeException e) {
            oldTree.close();
            cachedTree = null;
            return parser.parseString(null, fullText);
        }
    }

    static TSInputEdit computeInputEdit(String oldText, String newText) {
        int oldLen = (int) oldText.codePoints().count();
        int newLen = (int) newText.codePoints().count();

        int commonPrefix = computeCommonPrefixLength(oldText, newText);

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
}
