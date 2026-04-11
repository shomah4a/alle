package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.input.FileAttributes;
import io.github.shomah4a.alle.core.styling.FaceName;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * TreeDiredModelの状態からバッファの表示テキストを生成し、face を適用する。
 * ls -la 形式で表示する。
 */
public final class TreeDiredRenderer {

    private static final String EXPANDED_MARKER = "▼ ";
    private static final String COLLAPSED_MARKER = "▶ ";
    private static final String FILE_MARKER = "  ";
    private static final String MARKED_PREFIX = "* ";
    private static final String UNMARKED_PREFIX = "  ";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PERM_WIDTH = 10; // "drwxr-xr-x"
    private static final int TIMESTAMP_WIDTH = 16; // "2025-03-29 13:06"

    private TreeDiredRenderer() {}

    /**
     * バッファの内容をツリー表示で更新する。
     * バッファは read-only が一時的に解除された状態で呼ばれることを前提とする。
     */
    public static void render(
            BufferFacade buffer,
            Path rootDirectory,
            ListIterable<TreeDiredEntry> entries,
            ZoneId zoneId,
            ListIterable<DiredCustomColumn> customColumns,
            String rootSuffix) {
        String text = buildText(rootDirectory, entries, zoneId, customColumns, rootSuffix);

        int currentLength = buffer.length();
        if (currentLength > 0) {
            buffer.deleteText(0, currentLength);
        }
        buffer.insertText(0, text);

        int textLength = buffer.length();
        if (textLength > 0) {
            buffer.removeFace(0, textLength);
        }
        applyFaces(buffer, rootDirectory, entries, zoneId, customColumns, rootSuffix);
    }

    /**
     * ツリー表示のテキストを生成する。
     */
    static String buildText(
            Path rootDirectory,
            ListIterable<TreeDiredEntry> entries,
            ZoneId zoneId,
            ListIterable<DiredCustomColumn> customColumns,
            String rootSuffix) {
        var sb = new StringBuilder();
        sb.append(rootDirectory.toString());
        sb.append(':');
        if (!rootSuffix.isEmpty()) {
            sb.append(' ');
            sb.append(rootSuffix);
        }

        var widths = computeColumnWidths(entries, customColumns);

        // ヘッダ行
        sb.append('\n');
        sb.append("  ");
        sb.append(formatHeaderLine(widths, customColumns));

        // エントリ行
        for (TreeDiredEntry entry : entries) {
            sb.append('\n');
            sb.append(entry.isMarked() ? MARKED_PREFIX : UNMARKED_PREFIX);
            sb.append(formatEntryLine(entry, widths, zoneId, customColumns));
        }
        return sb.toString();
    }

    private static String formatHeaderLine(ColumnWidths widths, ListIterable<DiredCustomColumn> customColumns) {
        var sb = new StringBuilder();
        sb.append(padRight("perm", PERM_WIDTH));
        sb.append(' ');
        sb.append(padRight("owner", widths.ownerWidth));
        sb.append(' ');
        sb.append(padRight("group", widths.groupWidth));
        sb.append(' ');
        sb.append(padLeft("size", widths.sizeWidth));
        sb.append(' ');
        sb.append(padRight("mtime", TIMESTAMP_WIDTH));
        for (int i = 0; i < customColumns.size(); i++) {
            sb.append(' ');
            sb.append(padRight(customColumns.get(i).header(), widths.customWidths.get(i)));
        }
        sb.append(' ');
        sb.append("name");
        return sb.toString();
    }

    private static String formatEntryLine(
            TreeDiredEntry entry, ColumnWidths widths, ZoneId zoneId, ListIterable<DiredCustomColumn> customColumns) {
        var sb = new StringBuilder();
        FileAttributes attrs = entry.attributes();

        // パーミッション (d/- プレフィックス付き)
        sb.append(entry.isDirectory() ? 'd' : '-');
        sb.append(attrs.permissions());

        // オーナー
        sb.append(' ');
        sb.append(padRight(attrs.owner(), widths.ownerWidth));

        // グループ
        sb.append(' ');
        sb.append(padRight(attrs.group(), widths.groupWidth));

        // サイズ（右寄せ）
        sb.append(' ');
        sb.append(padLeft(String.valueOf(attrs.size()), widths.sizeWidth));

        // タイムスタンプ
        sb.append(' ');
        sb.append(TIMESTAMP_FORMAT.withZone(zoneId).format(attrs.lastModified()));

        // カスタムカラム
        for (int i = 0; i < customColumns.size(); i++) {
            sb.append(' ');
            String cellValue = customColumns.get(i).renderer().apply(entry.path());
            sb.append(padRight(cellValue, widths.customWidths.get(i)));
        }

        // インデント + マーカー + ファイル名
        sb.append(' ');
        sb.append("  ".repeat(entry.depth()));
        if (entry.isDirectory()) {
            sb.append(entry.isExpanded() ? EXPANDED_MARKER : COLLAPSED_MARKER);
            sb.append(entry.path().getFileName().toString());
            sb.append('/');
        } else {
            sb.append(FILE_MARKER);
            sb.append(entry.path().getFileName().toString());
        }

        return sb.toString();
    }

    private static void applyFaces(
            BufferFacade buffer,
            Path rootDirectory,
            ListIterable<TreeDiredEntry> entries,
            ZoneId zoneId,
            ListIterable<DiredCustomColumn> customColumns,
            String rootSuffix) {
        // ヘッダ行（ルートパス行）
        int headerLength = rootDirectory.toString().length() + 1; // +1 for ":"
        if (!rootSuffix.isEmpty()) {
            headerLength += 1 + rootSuffix.length(); // +1 for space
        }
        if (headerLength > 0) {
            buffer.putFace(0, headerLength, FaceName.HEADING);
        }

        var widths = computeColumnWidths(entries, customColumns);

        // カラムヘッダ行をスキップ
        String columnHeader = "  " + formatHeaderLine(widths, customColumns);
        int offset = headerLength + 1 + (int) columnHeader.codePoints().count(); // +1 for newline

        // 各エントリの行を走査してfaceを適用
        for (TreeDiredEntry entry : entries) {
            offset++; // 改行分
            int lineStart = offset;
            String prefix = entry.isMarked() ? MARKED_PREFIX : UNMARKED_PREFIX;
            String lineContent = prefix + formatEntryLine(entry, widths, zoneId, customColumns);
            int lineCodePoints = (int) lineContent.codePoints().count();

            if (entry.isMarked()) {
                buffer.putFace(lineStart, lineStart + lineCodePoints, FaceName.MARKED);
            }

            if (entry.isDirectory()) {
                String fileName = entry.path().getFileName().toString() + "/";
                int fileNameCodePoints = (int) fileName.codePoints().count();
                int nameEnd = lineStart + lineCodePoints;
                int nameStart = nameEnd - fileNameCodePoints;
                buffer.putFace(nameStart, nameEnd, FaceName.DIRECTORY);
            }

            offset = lineStart + lineCodePoints;
        }
    }

    private static ColumnWidths computeColumnWidths(
            ListIterable<TreeDiredEntry> entries, ListIterable<DiredCustomColumn> customColumns) {
        int maxOwner = "owner".length();
        int maxGroup = "group".length();
        int maxSize = "size".length();
        MutableList<Integer> customWidths = Lists.mutable.withInitialCapacity(customColumns.size());
        for (int i = 0; i < customColumns.size(); i++) {
            customWidths.add(customColumns.get(i).header().length());
        }
        for (TreeDiredEntry entry : entries) {
            FileAttributes attrs = entry.attributes();
            maxOwner = Math.max(maxOwner, attrs.owner().length());
            maxGroup = Math.max(maxGroup, attrs.group().length());
            maxSize = Math.max(maxSize, String.valueOf(attrs.size()).length());
            for (int i = 0; i < customColumns.size(); i++) {
                String cellValue = customColumns.get(i).renderer().apply(entry.path());
                customWidths.set(i, Math.max(customWidths.get(i), cellValue.length()));
            }
        }
        return new ColumnWidths(maxOwner, maxGroup, maxSize, customWidths);
    }

    private static String padRight(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return s + " ".repeat(width - s.length());
    }

    private static String padLeft(String s, int width) {
        if (s.length() >= width) {
            return s;
        }
        return " ".repeat(width - s.length()) + s;
    }

    private record ColumnWidths(int ownerWidth, int groupWidth, int sizeWidth, ListIterable<Integer> customWidths) {}
}
