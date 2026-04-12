package io.github.shomah4a.alle.core.mode.modes.makefile;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.window.Window;
import java.util.regex.Pattern;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * Makefile向けのインデント状態管理。
 *
 * <p>tree-sitterを使わず、行テキストの正規表現ベースで判定を行う。
 *
 * <p>インデントルール:
 * <ul>
 *   <li>ターゲット行（変数代入を除外した上でコロン判定）の次行はタブでインデント</li>
 *   <li>レシピ行（タブで始まる行）の次行もタブ継続</li>
 *   <li>それ以外はインデントなし</li>
 * </ul>
 *
 * <p>サイクルインデント: インデントなし / インデント1段 のサイクル。
 * インデント文字はバッファの {@code indent-tabs-mode} 設定に従い、
 * タブ文字またはスペースを使用する。
 */
public class MakefileIndentState {

    /** 変数代入行のパターン。ターゲット行判定から除外するために使用する。 */
    private static final Pattern ASSIGNMENT_PATTERN =
            Pattern.compile("^\\s*[A-Za-z_][A-Za-z0-9_]*\\s*(?::=|\\?=|\\+=|=)");

    /** ターゲット行のパターン。変数代入を除外した上でコロンを検出する。 */
    private static final Pattern TARGET_PATTERN = Pattern.compile("^[^\\t#=]*[^:=?+]:[^=]|^[^\\t#=]*[^:=?+]:$");

    /** レシピ行のパターン。タブで始まる行。 */
    private static final Pattern RECIPE_PATTERN = Pattern.compile("^\t");

    private int lastIndentLine = -1;
    private int lastIndentCycle = 0;

    /**
     * インデントサイクルを実行する。
     *
     * <p>インデントなし → タブ1つ → インデントなし のサイクル。
     *
     * @param window ウィンドウ
     * @param direction 循環方向。+1で順方向、-1で逆方向
     */
    public void cycleIndent(Window window, int direction) {
        var buf = window.getBuffer();
        int point = window.getPoint();
        int lineIndex = buf.lineIndexForOffset(point);
        String lineText = buf.lineText(lineIndex);
        String currentIndent = getLeadingWhitespace(lineText);
        ImmutableList<String> candidates = buildCycleCandidates(buf);

        int cycleIndex;
        if (lastIndentLine == lineIndex && lastIndentCycle >= 0 && lastIndentCycle < candidates.size()) {
            cycleIndex = Math.floorMod(lastIndentCycle + direction, candidates.size());
        } else {
            int idx = candidates.indexOf(currentIndent);
            if (idx >= 0) {
                cycleIndex = Math.floorMod(idx + direction, candidates.size());
            } else {
                cycleIndex = direction == 1 ? 0 : candidates.size() - 1;
            }
        }

        String newIndent = candidates.get(cycleIndex);
        setLineIndent(window, buf, lineIndex, currentIndent, newIndent);
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
        String lineText = buf.lineText(lineIndex);

        String indent = computeNewlineIndent(lineText, indentString(buf));
        window.insert("\n" + indent);
    }

    private String computeNewlineIndent(String currentLineText, String oneIndent) {
        if (isTargetLine(currentLineText)) {
            return oneIndent;
        }
        if (isRecipeLine(currentLineText)) {
            return oneIndent;
        }
        return "";
    }

    static boolean isTargetLine(String lineText) {
        if (ASSIGNMENT_PATTERN.matcher(lineText).find()) {
            return false;
        }
        return TARGET_PATTERN.matcher(lineText).find();
    }

    static boolean isRecipeLine(String lineText) {
        return RECIPE_PATTERN.matcher(lineText).find();
    }

    private static ImmutableList<String> buildCycleCandidates(BufferFacade buf) {
        return Lists.immutable.of("", indentString(buf));
    }

    private static String indentString(BufferFacade buf) {
        boolean useTabs = buf.getSettings().get(EditorSettings.INDENT_TABS_MODE);
        if (useTabs) {
            return "\t";
        }
        int width = buf.getSettings().get(EditorSettings.INDENT_WIDTH);
        return " ".repeat(width);
    }

    private static String getLeadingWhitespace(String text) {
        int i = 0;
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        return text.substring(0, i);
    }

    private void setLineIndent(Window window, BufferFacade buf, int lineIndex, String oldIndent, String newIndent) {
        if (oldIndent.equals(newIndent)) {
            return;
        }
        int lineStart = buf.lineStartOffset(lineIndex);
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
