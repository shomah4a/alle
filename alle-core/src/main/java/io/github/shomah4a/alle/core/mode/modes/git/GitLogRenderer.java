package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.styling.FaceName;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.eclipse.collections.api.list.ListIterable;

/**
 * git-log バッファの表示テキストを組み立てる。
 * short-header 形式 (Emacs vc-log 準拠):
 *
 * <pre>
 * commit &lt;shortHash&gt;
 * Author: &lt;author&gt;
 * Date:   &lt;yyyy-MM-dd&gt;
 *
 *     &lt;subject-truncated&gt;
 *     &lt;body 数行、末尾 truncate&gt;
 *
 * ----
 * commit ...
 * </pre>
 *
 * subject / body 各行の幅と body の行数は {@link RenderConfig} で制御する。
 */
public final class GitLogRenderer {

    private static final String SEPARATOR = "----";
    private static final String INDENT = "    ";
    private static final String ELLIPSIS = "…";

    private GitLogRenderer() {}

    /**
     * バッファの内容を git-log で書き換える (初回描画用)。
     * バッファは read-only を一時的に解除された状態で呼ばれることを前提とする。
     */
    public static void render(BufferFacade buffer, ListIterable<GitLogEntry> entries, RenderConfig config) {
        String text = buildText(entries, config);
        int currentLength = buffer.length();
        if (currentLength > 0) {
            buffer.removeFace(0, currentLength);
            buffer.deleteText(0, currentLength);
        }
        if (!text.isEmpty()) {
            buffer.insertText(0, text);
            applyFacesFromOffset(buffer, 0, text);
        }
    }

    /**
     * バッファ末尾に追加エントリを追記する (追加取得型ページネーション用)。
     * バッファが空でなければ先頭に区切り線を挟む。
     * バッファは read-only を一時的に解除された状態で呼ばれることを前提とする。
     */
    public static void appendEntries(BufferFacade buffer, ListIterable<GitLogEntry> entries, RenderConfig config) {
        if (entries.isEmpty()) {
            return;
        }
        var sb = new StringBuilder();
        if (buffer.length() > 0) {
            sb.append('\n').append(SEPARATOR).append('\n');
        }
        appendEntriesTo(sb, entries, config);
        int appendStart = buffer.length();
        String appended = sb.toString();
        buffer.insertText(appendStart, appended);
        applyFacesFromOffset(buffer, appendStart, appended);
    }

    static String buildText(ListIterable<GitLogEntry> entries, RenderConfig config) {
        var sb = new StringBuilder();
        appendEntriesTo(sb, entries, config);
        return sb.toString();
    }

    private static void appendEntriesTo(StringBuilder sb, ListIterable<GitLogEntry> entries, RenderConfig config) {
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssZ").withZone(config.zoneId());
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append('\n').append(SEPARATOR).append('\n');
            }
            var entry = entries.get(i);
            sb.append("commit ").append(entry.shortHash()).append('\n');
            sb.append("Author: ").append(entry.author()).append('\n');
            sb.append("Date:   ")
                    .append(dateFormatter.format(entry.commitTime()))
                    .append('\n');
            sb.append('\n');
            sb.append(INDENT)
                    .append(truncate(entry.subject(), config.subjectMaxWidth()))
                    .append('\n');
            appendBody(sb, entry.body(), config);
        }
    }

    private static void appendBody(StringBuilder sb, String body, RenderConfig config) {
        String trimmed = trimSurroundingBlankLines(body);
        if (trimmed.isEmpty()) {
            return;
        }
        String[] lines = trimmed.split("\n", -1);
        int limit = Math.min(lines.length, config.bodyMaxLines());
        for (int i = 0; i < limit; i++) {
            sb.append(INDENT)
                    .append(truncate(lines[i], config.subjectMaxWidth()))
                    .append('\n');
        }
        if (lines.length > limit) {
            sb.append(INDENT).append(ELLIPSIS).append('\n');
        }
    }

    private static String truncate(String source, int maxWidth) {
        if (source.length() <= maxWidth) {
            return source;
        }
        int cut = Math.max(0, maxWidth - ELLIPSIS.length());
        return source.substring(0, cut) + ELLIPSIS;
    }

    /**
     * 挿入したテキストの各行のラベル部分に face を適用する。
     * オフセットは codepoint 単位 (buffer.length と揃える)。
     * ラベル / 区切り線は ASCII のみで構成されるため char 数 = codepoint 数で問題ない。
     * 行末までの codepoint 数のみ全体 (subject / body) 側で計算する。
     */
    private static void applyFacesFromOffset(BufferFacade buffer, int startOffset, String text) {
        int offset = startOffset;
        for (var line : text.split("\n", -1)) {
            int lineLength = line.codePointCount(0, line.length());
            int lineEnd = offset + lineLength;
            if (line.startsWith("commit ")) {
                buffer.putFace(offset, lineEnd, FaceName.HEADING);
            } else if (line.startsWith("Author:")) {
                buffer.putFace(offset, offset + "Author:".length(), FaceName.KEYWORD);
            } else if (line.startsWith("Date:")) {
                buffer.putFace(offset, offset + "Date:".length(), FaceName.KEYWORD);
            } else if (line.equals(SEPARATOR)) {
                buffer.putFace(offset, lineEnd, FaceName.COMMENT);
            }
            offset = lineEnd + 1;
        }
    }

    private static String trimSurroundingBlankLines(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && (value.charAt(start) == '\n' || value.charAt(start) == '\r')) {
            start++;
        }
        while (end > start && (value.charAt(end - 1) == '\n' || value.charAt(end - 1) == '\r')) {
            end--;
        }
        return value.substring(start, end);
    }

    /**
     * 表示制御のパラメータ。
     *
     * @param subjectMaxWidth subject と body 各行の表示幅上限 (これを超えると末尾に "…" を付けて truncate)
     * @param bodyMaxLines body から表示する行数上限 (超過分は "…" 1 行で省略)
     * @param zoneId Date 行の日付を計算するタイムゾーン
     */
    public record RenderConfig(int subjectMaxWidth, int bodyMaxLines, ZoneId zoneId) {

        public static RenderConfig defaults() {
            return new RenderConfig(80, 3, ZoneId.systemDefault());
        }
    }
}
