package io.github.shomah4a.alle.core.mode.indent;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzer;
import io.github.shomah4a.alle.core.syntax.SyntaxNode;
import io.github.shomah4a.alle.core.syntax.SyntaxTree;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.jspecify.annotations.Nullable;

/**
 * Cスタイルインデントの状態管理。
 * インデントサイクルとnewline-and-indentのロジックを提供する。
 *
 * <p>括弧判定はすべてtree-sitterのASTに基づいて行う。
 * 行末の開き括弧判定は{@link SyntaxTree#nodeAt(int, int)}でトークンを取得し、
 * コメントノードをスキップして意味のある最後のトークンが開き括弧かを判定する。
 */
public class CStyleIndentState {

    private static final Pattern LEADING_WHITESPACE = Pattern.compile("^(\\s*)");
    private static final String COMMENT_NODE_TYPE = "comment";
    private static final String[] BRACKET_TOKENS = {"(", ")", "[", "]", "{", "}", ","};

    private final CStyleIndentConfig config;
    private final SyntaxAnalyzer syntaxAnalyzer;

    private int lastIndentLine = -1;
    private int lastIndentCycle = 0;

    public CStyleIndentState(CStyleIndentConfig config, SyntaxAnalyzer syntaxAnalyzer) {
        this.config = config;
        this.syntaxAnalyzer = syntaxAnalyzer;
    }

    /**
     * インデントサイクルを実行する。
     *
     * @param window ウィンドウ
     * @param direction 循環方向。+1で順方向、-1で逆方向
     */
    public void cycleIndent(Window window, int direction) {
        var buf = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buf.lineIndexForOffset(point);

        int prevIndentLen = 0;
        boolean prevLineEndsWithOpenBracket = false;
        if (lineIndex > 0) {
            String prevLineText = buf.lineText(lineIndex - 1);
            prevIndentLen = getIndent(prevLineText).length();
            SyntaxTree tree = analyzeBuffer(buf);
            prevLineEndsWithOpenBracket = isOpenBracketBeforeColumn(tree, lineIndex - 1, prevLineText.length());
        }

        int currentIndentLen = getIndent(buf.lineText(lineIndex)).length();

        Integer bracketIndent = getBracketIndent(buf, lineIndex, 0);

        MutableList<Integer> candidates =
                buildIndentCandidates(prevIndentLen, bracketIndent, prevLineEndsWithOpenBracket);

        int cycleIndex;
        if (lastIndentLine == lineIndex && lastIndentCycle >= 0 && lastIndentCycle < candidates.size()) {
            cycleIndex = Math.floorMod(lastIndentCycle + direction, candidates.size());
        } else {
            int idx = candidates.indexOf(currentIndentLen);
            if (idx >= 0) {
                cycleIndex = Math.floorMod(idx + direction, candidates.size());
            } else {
                cycleIndex = direction == 1 ? 0 : candidates.size() - 1;
            }
        }

        String newIndent = " ".repeat(candidates.get(cycleIndex));
        setLineIndent(window, buf, lineIndex, newIndent);
        lastIndentLine = lineIndex;
        lastIndentCycle = cycleIndex;
    }

    /**
     * 改行を挿入し、適切なインデントを付与する。
     *
     * @param window ウィンドウ
     */
    public void newlineAndIndent(Window window) {
        var buf = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buf.lineIndexForOffset(point);
        int lineStart = buf.lineStartOffset(lineIndex);

        String textBeforeCursor = buf.substring(lineStart, point);
        String indent = getIndent(textBeforeCursor);

        int col = point - lineStart;
        Integer bracketIndent = getBracketIndent(buf, lineIndex, col);

        if (bracketIndent != null) {
            indent = " ".repeat(bracketIndent);
        } else {
            SyntaxTree tree = analyzeBuffer(buf);
            if (isOpenBracketBeforeColumn(tree, lineIndex, col)) {
                indent += " ".repeat(config.indentWidth());
            }
        }

        window.insert("\n" + indent);
    }

    /**
     * 指定行の指定カラムより前にある、コメントを除いた最後のトークンが
     * 開き括弧文字であるかをASTで判定する。
     *
     * @param tree 構文木
     * @param lineIndex 対象行
     * @param column 判定対象の終了位置（この位置より前のトークンを対象とする）
     * @return 開き括弧で終わる場合true
     */
    private boolean isOpenBracketBeforeColumn(SyntaxTree tree, int lineIndex, int column) {
        int searchCol = column;
        // 行末（または指定位置）から後方に向かってトークンを探索する。
        // コメントノードはスキップし、意味のある最後のトークンが開き括弧かを判定する。
        while (searchCol > 0) {
            searchCol--;
            Optional<SyntaxNode> nodeOpt = tree.nodeAt(lineIndex, searchCol);
            if (nodeOpt.isEmpty()) {
                continue;
            }
            SyntaxNode node = nodeOpt.get();
            if (COMMENT_NODE_TYPE.equals(node.type())) {
                // コメントノードの開始位置より前にジャンプする
                searchCol = node.startColumn();
                continue;
            }
            // 意味のあるトークンが見つかった
            return isOpenBracketToken(node);
        }
        return false;
    }

    /**
     * ノードのtypeが開き括弧文字の1文字トークンであるかを判定する。
     */
    private boolean isOpenBracketToken(SyntaxNode node) {
        String type = node.type();
        if (type.length() != 1) {
            return false;
        }
        return config.openBrackets().contains(type.charAt(0));
    }

    private static String getIndent(String text) {
        var matcher = LEADING_WHITESPACE.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private void setLineIndent(Window window, BufferFacade buf, int lineIndex, String newIndent) {
        int lineStart = buf.lineStartOffset(lineIndex);
        String lineText = buf.lineText(lineIndex);
        String oldIndent = getIndent(lineText);
        if (oldIndent.equals(newIndent)) {
            return;
        }
        int oldLen = oldIndent.length();
        int point = window.getPoint();
        buf.deleteText(lineStart, oldLen);
        buf.insertText(lineStart, newIndent);
        if (point < lineStart + oldLen) {
            window.setPoint(lineStart + newIndent.length());
        } else {
            window.setPoint(point - oldLen + newIndent.length());
        }
    }

    private @Nullable Integer getBracketIndent(BufferFacade buf, int lineIndex, int column) {
        SyntaxTree tree = analyzeBuffer(buf);
        Optional<SyntaxNode> bracket = tree.enclosingBracket(lineIndex, column);
        if (bracket.isEmpty()) {
            return null;
        }
        SyntaxNode node = bracket.get();
        int bracketStartLine = node.startLine();
        int bracketStartCol = node.startColumn();
        int bracketEndLine = node.endLine();

        if (lineIndex == bracketStartLine && column <= bracketStartCol) {
            return null;
        }
        if (lineIndex > bracketEndLine) {
            return null;
        }

        SyntaxNode firstContent = findFirstContentChild(node);
        if (firstContent != null && firstContent.startLine() == bracketStartLine) {
            if (lineIndex == bracketStartLine && column <= firstContent.startColumn()) {
                String bracketLineText = buf.lineText(bracketStartLine);
                int bracketLineIndent = getIndent(bracketLineText).length();
                return bracketLineIndent + config.indentWidth();
            }
            return firstContent.startColumn();
        }

        String bracketLineText = buf.lineText(bracketStartLine);
        int bracketLineIndent = getIndent(bracketLineText).length();
        return bracketLineIndent + config.indentWidth();
    }

    private static @Nullable SyntaxNode findFirstContentChild(SyntaxNode node) {
        for (int i = 0; i < node.children().size(); i++) {
            SyntaxNode child = node.children().get(i);
            if (isSkippableToken(child.type())) {
                continue;
            }
            return child;
        }
        return null;
    }

    /**
     * 括弧内の最初の意味のある子を探索する際にスキップすべきトークンかを判定する。
     * 括弧トークンとコメントをスキップする。
     */
    private static boolean isSkippableToken(String type) {
        if (COMMENT_NODE_TYPE.equals(type)) {
            return true;
        }
        for (String token : BRACKET_TOKENS) {
            if (token.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private SyntaxTree analyzeBuffer(BufferFacade buf) {
        var lines = Lists.mutable.<String>withInitialCapacity(buf.lineCount());
        for (int i = 0; i < buf.lineCount(); i++) {
            lines.add(buf.lineText(i));
        }
        return syntaxAnalyzer.analyze(lines);
    }

    private MutableList<Integer> buildIndentCandidates(
            int prevIndentLen, @Nullable Integer bracketIndent, boolean prevLineEndsWithOpenBracket) {
        MutableList<Integer> candidates = Lists.mutable.empty();
        MutableSet<Integer> seen = Sets.mutable.empty();

        if (bracketIndent != null) {
            candidates.add(bracketIndent);
            seen.add(bracketIndent);
        }

        // 前行が開き括弧で終わる場合、インデント増加を候補に含める
        if (prevLineEndsWithOpenBracket) {
            int increased = prevIndentLen + config.indentWidth();
            if (seen.add(increased)) {
                candidates.add(increased);
            }
        }

        int[] levels = {prevIndentLen, Math.max(prevIndentLen - config.indentWidth(), 0), 0};
        for (int level : levels) {
            if (seen.add(level)) {
                candidates.add(level);
            }
        }
        return candidates;
    }
}
