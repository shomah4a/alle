package io.github.shomah4a.alle.core.mode.modes.dired;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.input.FileAttributes;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeDiredRendererTest {

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final Instant TIME = Instant.parse("2025-03-29T13:06:00Z");
    private static final FileAttributes DIR_ATTRS = new FileAttributes("rwxr-xr-x", 2, "shoma", "shoma", 4096, TIME);
    private static final FileAttributes FILE_ATTRS = new FileAttributes("rw-r--r--", 1, "shoma", "shoma", 916, TIME);
    private static final ListIterable<DiredCustomColumn> NO_CUSTOM_COLUMNS = Lists.immutable.empty();
    private static final String NO_SUFFIX = "";

    @Nested
    class テキスト生成 {

        @Test
        void ヘッダ行にルートディレクトリとカラムヘッダが表示される() {
            ListIterable<TreeDiredEntry> entries = Lists.immutable.empty();
            String text = TreeDiredRenderer.buildText(Path.of("/project"), entries, UTC, NO_CUSTOM_COLUMNS, NO_SUFFIX);
            var lines = text.lines().toList();

            assertEquals("/project:", lines.get(0));
            assertEquals("  perm       owner group size mtime            name", lines.get(1));
        }

        @Test
        void ファイルとディレクトリがls形式で表示される() {
            var entries = Lists.immutable.of(
                    new TreeDiredEntry(Path.of("/p/src"), 0, true, false, false, DIR_ATTRS),
                    new TreeDiredEntry(Path.of("/p/README.md"), 0, false, false, false, FILE_ATTRS));

            String text = TreeDiredRenderer.buildText(Path.of("/p"), entries, UTC, NO_CUSTOM_COLUMNS, NO_SUFFIX);
            var lines = text.lines().toList();

            assertEquals("/p:", lines.get(0));
            assertEquals("  drwxr-xr-x shoma shoma 4096 2025-03-29 13:06 ▶ src/", lines.get(2));
            assertEquals("  -rw-r--r-- shoma shoma  916 2025-03-29 13:06   README.md", lines.get(3));
        }

        @Test
        void 展開済みディレクトリには下向き三角が表示される() {
            var entries = Lists.immutable.of(
                    new TreeDiredEntry(Path.of("/p/src"), 0, true, true, false, DIR_ATTRS),
                    new TreeDiredEntry(Path.of("/p/src/Main.java"), 1, false, false, false, FILE_ATTRS));

            String text = TreeDiredRenderer.buildText(Path.of("/p"), entries, UTC, NO_CUSTOM_COLUMNS, NO_SUFFIX);
            var lines = text.lines().toList();

            assertEquals("  drwxr-xr-x shoma shoma 4096 2025-03-29 13:06 ▼ src/", lines.get(2));
            assertEquals("  -rw-r--r-- shoma shoma  916 2025-03-29 13:06     Main.java", lines.get(3));
        }

        @Test
        void ネストしたエントリのファイル名前にインデントが入る() {
            var entries = Lists.immutable.of(
                    new TreeDiredEntry(Path.of("/p/a"), 0, true, true, false, DIR_ATTRS),
                    new TreeDiredEntry(Path.of("/p/a/b"), 1, true, true, false, DIR_ATTRS),
                    new TreeDiredEntry(Path.of("/p/a/b/c.txt"), 2, false, false, false, FILE_ATTRS));

            String text = TreeDiredRenderer.buildText(Path.of("/p"), entries, UTC, NO_CUSTOM_COLUMNS, NO_SUFFIX);
            var lines = text.lines().toList();

            assertEquals("  drwxr-xr-x shoma shoma 4096 2025-03-29 13:06 ▼ a/", lines.get(2));
            assertEquals("  drwxr-xr-x shoma shoma 4096 2025-03-29 13:06   ▼ b/", lines.get(3));
            assertEquals("  -rw-r--r-- shoma shoma  916 2025-03-29 13:06       c.txt", lines.get(4));
        }

        @Test
        void マーク済みエントリの行頭にアスタリスクが表示される() {
            var entries = Lists.immutable.of(
                    new TreeDiredEntry(Path.of("/p/a.txt"), 0, false, false, true, FILE_ATTRS),
                    new TreeDiredEntry(Path.of("/p/b.txt"), 0, false, false, false, FILE_ATTRS));

            String text = TreeDiredRenderer.buildText(Path.of("/p"), entries, UTC, NO_CUSTOM_COLUMNS, NO_SUFFIX);
            var lines = text.lines().toList();

            assertEquals("* -rw-r--r-- shoma shoma  916 2025-03-29 13:06   a.txt", lines.get(2));
            assertEquals("  -rw-r--r-- shoma shoma  916 2025-03-29 13:06   b.txt", lines.get(3));
        }
    }

    @Nested
    class カスタムカラム {

        @Test
        void カスタムカラムがmtimeとnameの間に表示される() {
            var entries =
                    Lists.immutable.of(new TreeDiredEntry(Path.of("/p/a.txt"), 0, false, false, false, FILE_ATTRS));
            var customColumns = Lists.immutable.of(DiredCustomColumn.of("git", path -> "M"));

            String text = TreeDiredRenderer.buildText(Path.of("/p"), entries, UTC, customColumns, NO_SUFFIX);
            var lines = text.lines().toList();

            assertEquals("  perm       owner group size mtime            git name", lines.get(1));
            assertEquals("  -rw-r--r-- shoma shoma  916 2025-03-29 13:06 M     a.txt", lines.get(2));
        }

        @Test
        void 複数のカスタムカラムが順番に表示される() {
            var entries =
                    Lists.immutable.of(new TreeDiredEntry(Path.of("/p/a.txt"), 0, false, false, false, FILE_ATTRS));
            var customColumns = Lists.immutable.of(
                    DiredCustomColumn.of("git", path -> "M"), DiredCustomColumn.of("flag", path -> "★"));

            String text = TreeDiredRenderer.buildText(Path.of("/p"), entries, UTC, customColumns, NO_SUFFIX);
            var lines = text.lines().toList();

            assertEquals("  perm       owner group size mtime            git flag name", lines.get(1));
        }

        @Test
        void カスタムカラムの幅がヘッダとセルの最大値に合わせられる() {
            var entries = Lists.immutable.of(
                    new TreeDiredEntry(Path.of("/p/a.txt"), 0, false, false, false, FILE_ATTRS),
                    new TreeDiredEntry(Path.of("/p/b.txt"), 0, false, false, false, FILE_ATTRS));
            var customColumns = Lists.immutable.of(DiredCustomColumn.of("st", path -> {
                if (path.getFileName().toString().equals("a.txt")) {
                    return "modified";
                }
                return "M";
            }));

            String text = TreeDiredRenderer.buildText(Path.of("/p"), entries, UTC, customColumns, NO_SUFFIX);
            var lines = text.lines().toList();

            // "modified" (8文字) が最大幅となり、ヘッダ "st" も8幅にパディングされる
            assertEquals("  perm       owner group size mtime            st       name", lines.get(1));
            assertEquals("  -rw-r--r-- shoma shoma  916 2025-03-29 13:06 modified   a.txt", lines.get(2));
            assertEquals("  -rw-r--r-- shoma shoma  916 2025-03-29 13:06 M          b.txt", lines.get(3));
        }
    }

    @Nested
    class ルートサフィックス {

        @Test
        void ルート行にサフィックスが追加される() {
            ListIterable<TreeDiredEntry> entries = Lists.immutable.empty();
            String text = TreeDiredRenderer.buildText(Path.of("/project"), entries, UTC, NO_CUSTOM_COLUMNS, "[main]");
            var lines = text.lines().toList();

            assertEquals("/project: [main]", lines.get(0));
        }

        @Test
        void 空サフィックスの場合はルート行がそのまま() {
            ListIterable<TreeDiredEntry> entries = Lists.immutable.empty();
            String text = TreeDiredRenderer.buildText(Path.of("/project"), entries, UTC, NO_CUSTOM_COLUMNS, NO_SUFFIX);
            var lines = text.lines().toList();

            assertEquals("/project:", lines.get(0));
        }
    }
}
