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
 * <p>Python modeのPythonIndentStateのJava版。
 * 括弧文字を{@link CStyleIndentConfig}でカスタマイズ可能にしている。
 */
public class CStyleIndentState {

    private static final Pattern LEADING_WHITESPACE = Pattern.compile("^(\\s*)");
    private static final String[] BRACKET_TOKENS = {"(", ")", "[", "]", "{", "}", ","};

    private final CStyleIndentConfig config;
    private final @Nullable SyntaxAnalyzer syntaxAnalyzer;

    private int lastIndentLine = -1;
    private int lastIndentCycle = 0;

    public CStyleIndentState(CStyleIndentConfig config, @Nullable SyntaxAnalyzer syntaxAnalyzer) {
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
            prevLineEndsWithOpenBracket =
                    config.openBracketEndPattern().matcher(prevLineText).find();
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
        } else if (config.openBracketEndPattern().matcher(textBeforeCursor).find()) {
            indent += " ".repeat(config.indentWidth());
        }

        window.insert("\n" + indent);
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
        if (syntaxAnalyzer == null) {
            return null;
        }
        var lines = Lists.mutable.<String>withInitialCapacity(buf.lineCount());
        for (int i = 0; i < buf.lineCount(); i++) {
            lines.add(buf.lineText(i));
        }
        SyntaxTree tree = syntaxAnalyzer.analyze(lines);
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
            boolean isBracketToken = false;
            for (String token : BRACKET_TOKENS) {
                if (token.equals(child.type())) {
                    isBracketToken = true;
                    break;
                }
            }
            if (!isBracketToken) {
                return child;
            }
        }
        return null;
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
