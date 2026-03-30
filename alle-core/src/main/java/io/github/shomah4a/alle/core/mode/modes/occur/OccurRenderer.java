package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.styling.FaceName;
import org.eclipse.collections.api.list.ListIterable;

/**
 * OccurModelの状態からバッファの表示テキストを生成する。
 *
 * <pre>
 * 3 lines matching "query" in buffer-name
 *     10: マッチした行のテキスト
 *     25: 別のマッチ行
 *    103: さらに別のマッチ行
 * </pre>
 */
public final class OccurRenderer {

    private OccurRenderer() {}

    /**
     * バッファの内容をoccur結果で更新する。
     * バッファはread-onlyが一時的に解除された状態で呼ばれることを前提とする。
     */
    public static void render(BufferFacade buffer, OccurModel model) {
        String text = buildText(model);

        int currentLength = buffer.length();
        if (currentLength > 0) {
            buffer.deleteText(0, currentLength);
        }
        buffer.insertText(0, text);

        int textLength = buffer.length();
        if (textLength > 0) {
            buffer.removeFace(0, textLength);
        }
        applyFaces(buffer, model);
    }

    /**
     * occur結果のテキストを生成する。
     */
    static String buildText(OccurModel model) {
        ListIterable<OccurMatch> matches = model.getMatches();
        var sb = new StringBuilder();

        // ヘッダ行
        sb.append(matches.size());
        sb.append(" lines matching \"");
        sb.append(model.getQuery());
        sb.append("\" in ");
        sb.append(model.getSourceBufferName());

        if (matches.isEmpty()) {
            return sb.toString();
        }

        // 行番号の最大桁数（1始まり表示）
        int maxLineNumber = matches.getLast().lineIndex() + 1;
        int lineNumberWidth = String.valueOf(maxLineNumber).length();

        // マッチ行
        for (OccurMatch match : matches) {
            sb.append('\n');
            String lineNum = String.valueOf(match.lineIndex() + 1);
            // 右寄せ
            sb.append(" ".repeat(lineNumberWidth - lineNum.length() + 4));
            sb.append(lineNum);
            sb.append(": ");
            sb.append(match.lineText());
        }
        return sb.toString();
    }

    private static void applyFaces(BufferFacade buffer, OccurModel model) {
        ListIterable<OccurMatch> matches = model.getMatches();
        if (matches.isEmpty()) {
            return;
        }

        // ヘッダ行にHEADING faceを適用
        String headerLine = buildHeaderLine(model);
        int headerLength = (int) headerLine.codePoints().count();
        buffer.putFace(0, headerLength, FaceName.HEADING);

        // 各マッチ行の行番号部分にface適用
        int maxLineNumber = matches.getLast().lineIndex() + 1;
        int lineNumberWidth = String.valueOf(maxLineNumber).length();
        int offset = headerLength;

        for (OccurMatch match : matches) {
            offset++; // 改行分
            int lineStart = offset;
            String lineNum = String.valueOf(match.lineIndex() + 1);
            int padding = lineNumberWidth - lineNum.length() + 4;
            int lineNumEnd = lineStart + padding + lineNum.length() + 1; // +1 for ":"
            buffer.putFace(lineStart, lineNumEnd, FaceName.LINE_NUMBER);

            String fullLine = " ".repeat(padding) + lineNum + ": " + match.lineText();
            int lineCodePoints = (int) fullLine.codePoints().count();
            offset = lineStart + lineCodePoints;
        }
    }

    private static String buildHeaderLine(OccurModel model) {
        return model.getMatches().size()
                + " lines matching \""
                + model.getQuery()
                + "\" in "
                + model.getSourceBufferName();
    }
}
