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
 * YAML向けのASTベースインデント状態管理。
 *
 * <p>tree-sitter-yamlのASTノードタイプを使用してインデント判定を行う。
 *
 * <p>インデント増加の判定:
 * <ul>
 *   <li>行末のトークンがコロン{@code :}である場合（マッピングキーの後にネスト値がない）</li>
 *   <li>{@code flow_mapping}, {@code flow_sequence} 内（フロースタイル括弧）</li>
 * </ul>
 *
 * <p>インデントサイクルの候補は前行の状態から決定する。
 * 現在行のインデント変更はASTに影響するため、候補計算には
 * 「前行の末尾トークンがコロンか」という、現在行に依存しない判定を使用する。
 */
public class YamlIndentState {

    private static final Pattern LEADING_WHITESPACE = Pattern.compile("^(\\s*)");

    private final int indentWidth;
    private final SyntaxAnalyzer syntaxAnalyzer;

    private int lastIndentLine = -1;
    private int lastIndentCycle = 0;
    /** サイクル開始時に計算した候補リスト。同一行での連続サイクル中は再計算しない。 */
    private MutableList<Integer> lastCandidates = Lists.mutable.empty();

    public YamlIndentState(int indentWidth, SyntaxAnalyzer syntaxAnalyzer) {
        this.indentWidth = indentWidth;
        this.syntaxAnalyzer = syntaxAnalyzer;
    }

    /**
     * インデントサイクルを実行する。
     *
     * <p>同一行での連続サイクルでは初回に計算した候補リストを再利用する。
     * 現在行のインデント変更がASTに影響してサイクル候補が不安定になることを防ぐため。
     *
     * @param window ウィンドウ
     * @param direction 循環方向。+1で順方向、-1で逆方向
     */
    public void cycleIndent(Window window, int direction) {
        var buf = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buf.lineIndexForOffset(point);
        int currentIndentLen = getIndent(buf.lineText(lineIndex)).length();

        // 同一行での連続サイクル判定。前回と同じ行で、前回のサイクルインデックスが
        // キャッシュ済み候補リストの範囲内であれば、候補リストを再計算せず再利用する。
        // これにより、インデント変更→AST再パース→候補変化、という不安定化を防ぐ。
        boolean continuing =
                lastIndentLine == lineIndex && lastIndentCycle >= 0 && lastIndentCycle < lastCandidates.size();

        MutableList<Integer> candidates;
        if (continuing) {
            candidates = lastCandidates;
        } else {
            SyntaxTree tree = analyzeBuffer(buf);
            candidates = buildIndentCandidates(buf, tree, lineIndex);
            lastCandidates = candidates;
        }

        int cycleIndex;
        if (continuing) {
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

        String currentIndent = getIndent(textBeforeCursor);
        SyntaxTree tree = analyzeBuffer(buf);

        int col = point - lineStart;
        String indent = computeNewlineIndent(buf, tree, lineIndex, col, currentIndent);

        window.insert("\n" + indent);
    }

    private String computeNewlineIndent(
            BufferFacade buf, SyntaxTree tree, int lineIndex, int column, String currentIndent) {
        // フロースタイル括弧内ならその内部インデントに合わせる
        Integer flowIndent = getFlowBracketIndent(tree, lineIndex, column);
        if (flowIndent != null) {
            return " ".repeat(flowIndent);
        }

        // 現在行の末尾トークンがコロンならインデント増加
        String lineText = buf.lineText(lineIndex);
        if (isLineEndingWithColon(tree, lineIndex, lineText)) {
            return currentIndent + " ".repeat(indentWidth);
        }

        return currentIndent;
    }

    /**
     * 指定行の末尾の意味あるトークンがコロンであるかをASTで判定する。
     *
     * <p>前行の末尾がコロンかどうかは、現在行のインデント変更に影響されないため、
     * サイクル候補の安定性が保たれる。
     *
     * @param tree 構文木
     * @param lineIndex 対象行
     * @param lineText 対象行のテキスト
     * @return 末尾トークンがコロンの場合true
     */
    private boolean isLineEndingWithColon(SyntaxTree tree, int lineIndex, String lineText) {
        String stripped = lineText.stripTrailing();
        if (stripped.isEmpty()) {
            return false;
        }
        int lastCol = stripped.length() - 1;
        Optional<SyntaxNode> node = tree.nodeAt(lineIndex, lastCol);
        return node.isPresent() && ":".equals(node.get().type());
    }

    /**
     * フロースタイル括弧内の場合、括弧の開始カラム + 1 を返す。
     */
    private @Nullable Integer getFlowBracketIndent(SyntaxTree tree, int lineIndex, int column) {
        var bracket = tree.enclosingBracket(lineIndex, column);
        if (bracket.isEmpty()) {
            return null;
        }
        SyntaxNode node = bracket.get();
        if (lineIndex == node.startLine() && column <= node.startColumn()) {
            return null;
        }
        if (lineIndex > node.endLine()) {
            return null;
        }
        return node.startColumn() + 1;
    }

    private MutableList<Integer> buildIndentCandidates(BufferFacade buf, SyntaxTree tree, int lineIndex) {
        MutableList<Integer> candidates = Lists.mutable.empty();
        MutableSet<Integer> seen = Sets.mutable.empty();

        // フロースタイル括弧内なら括弧内インデント
        Integer flowIndent = getFlowBracketIndent(tree, lineIndex, 0);
        if (flowIndent != null) {
            candidates.add(flowIndent);
            seen.add(flowIndent);
        }

        // 前行の情報からの候補
        if (lineIndex > 0) {
            String prevLine = buf.lineText(lineIndex - 1);
            int prevIndentLen = getIndent(prevLine).length();

            // 前行の末尾がコロンならインデント増加候補
            if (isLineEndingWithColon(tree, lineIndex - 1, prevLine)) {
                int increased = prevIndentLen + indentWidth;
                if (seen.add(increased)) {
                    candidates.add(increased);
                }
            }

            // 前行と同じインデント
            if (seen.add(prevIndentLen)) {
                candidates.add(prevIndentLen);
            }

            // 前行より1段下げ
            int decreased = Math.max(prevIndentLen - indentWidth, 0);
            if (seen.add(decreased)) {
                candidates.add(decreased);
            }
        }

        // 0インデント
        if (seen.add(0)) {
            candidates.add(0);
        }

        return candidates;
    }

    private SyntaxTree analyzeBuffer(BufferFacade buf) {
        MutableList<String> lines = Lists.mutable.withInitialCapacity(buf.lineCount());
        for (int i = 0; i < buf.lineCount(); i++) {
            lines.add(buf.lineText(i));
        }
        return syntaxAnalyzer.analyze(lines);
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
}
