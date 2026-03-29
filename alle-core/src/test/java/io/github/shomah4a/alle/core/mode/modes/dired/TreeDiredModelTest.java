package io.github.shomah4a.alle.core.mode.modes.dired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.input.DirectoryEntry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FileAttributes;
import java.nio.file.Path;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeDiredModelTest {

    private static final FileAttributes A = FileAttributes.EMPTY;

    private static DirectoryLister stubLister(MutableMap<Path, ListIterable<DirectoryEntry>> entries) {
        return directory -> entries.getIfAbsentValue(directory, Lists.immutable.empty());
    }

    private static DirectoryEntry.Directory dir(String path) {
        return new DirectoryEntry.Directory(Path.of(path), A);
    }

    private static DirectoryEntry.File file(String path) {
        return new DirectoryEntry.File(Path.of(path), A);
    }

    @Nested
    class 初期状態 {

        @Test
        void ルートディレクトリ直下のエントリが表示される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(dir("/project/src"), file("/project/README.md")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            var visible = model.getVisibleEntries();

            assertEquals(2, visible.size());
            assertEquals(Path.of("/project/src"), visible.get(0).path());
            assertTrue(visible.get(0).isDirectory());
            assertFalse(visible.get(0).isExpanded());
            assertEquals(Path.of("/project/README.md"), visible.get(1).path());
            assertFalse(visible.get(1).isDirectory());
        }

        @Test
        void ルートディレクトリは展開済みである() {
            var model = new TreeDiredModel(Path.of("/project"), dir -> Lists.immutable.empty());

            assertTrue(model.isExpanded(Path.of("/project")));
        }
    }

    @Nested
    class ディレクトリの展開 {

        @Test
        void 展開すると子エントリが表示される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(dir("/project/src")));
            entries.put(Path.of("/project/src"), Lists.immutable.of(file("/project/src/Main.java")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.toggle(Path.of("/project/src"));
            var visible = model.getVisibleEntries();

            assertEquals(2, visible.size());
            assertEquals(Path.of("/project/src"), visible.get(0).path());
            assertTrue(visible.get(0).isExpanded());
            assertEquals(Path.of("/project/src/Main.java"), visible.get(1).path());
            assertEquals(1, visible.get(1).depth());
        }

        @Test
        void 折り畳むと子エントリが非表示になる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(dir("/project/src")));
            entries.put(Path.of("/project/src"), Lists.immutable.of(file("/project/src/Main.java")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.toggle(Path.of("/project/src"));
            model.toggle(Path.of("/project/src"));
            var visible = model.getVisibleEntries();

            assertEquals(1, visible.size());
            assertFalse(visible.get(0).isExpanded());
        }

        @Test
        void ルートディレクトリは折り畳めない() {
            var model = new TreeDiredModel(Path.of("/project"), dir -> Lists.immutable.empty());

            model.toggle(Path.of("/project"));

            assertTrue(model.isExpanded(Path.of("/project")));
        }
    }

    @Nested
    class ソート順 {

        @Test
        void ディレクトリがファイルより先に表示される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(file("/project/zebra.txt"), dir("/project/alpha")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            var visible = model.getVisibleEntries();

            assertTrue(visible.get(0).isDirectory());
            assertFalse(visible.get(1).isDirectory());
        }

        @Test
        void 同種のエントリは名前の大文字小文字を無視してソートされる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/project"),
                    Lists.immutable.of(
                            file("/project/Zebra.txt"), file("/project/apple.txt"), file("/project/Banana.txt")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            var visible = model.getVisibleEntries();

            assertEquals("apple.txt", visible.get(0).path().getFileName().toString());
            assertEquals("Banana.txt", visible.get(1).path().getFileName().toString());
            assertEquals("Zebra.txt", visible.get(2).path().getFileName().toString());
        }
    }

    @Nested
    class ルートディレクトリの変更 {

        @Test
        void 変更後は新しいルートのみ展開される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(dir("/project/src")));
            entries.put(Path.of("/parent"), Lists.immutable.of(dir("/parent/project")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.toggle(Path.of("/project/src"));
            model.setRootDirectory(Path.of("/parent"));
            var visible = model.getVisibleEntries();

            assertEquals(1, visible.size());
            assertFalse(model.isExpanded(Path.of("/project/src")));
        }
    }

    @Nested
    class マーク機能 {

        @Test
        void markしたパスがisMarkedでtrueになる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(file("/project/a.txt")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.mark(Path.of("/project/a.txt"));

            assertTrue(model.isMarked(Path.of("/project/a.txt")));
        }

        @Test
        void unmarkしたパスがisMarkedでfalseになる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(file("/project/a.txt")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.mark(Path.of("/project/a.txt"));
            model.unmark(Path.of("/project/a.txt"));

            assertFalse(model.isMarked(Path.of("/project/a.txt")));
        }

        @Test
        void toggleMarkでマーク状態が反転する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(file("/project/a.txt")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.toggleMark(Path.of("/project/a.txt"));
            assertTrue(model.isMarked(Path.of("/project/a.txt")));

            model.toggleMark(Path.of("/project/a.txt"));
            assertFalse(model.isMarked(Path.of("/project/a.txt")));
        }

        @Test
        void clearMarksで全マークがクリアされる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(file("/project/a.txt"), file("/project/b.txt")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.mark(Path.of("/project/a.txt"));
            model.mark(Path.of("/project/b.txt"));
            model.clearMarks();

            assertFalse(model.isMarked(Path.of("/project/a.txt")));
            assertFalse(model.isMarked(Path.of("/project/b.txt")));
        }

        @Test
        void getVisibleEntriesがマーク状態を含む() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(file("/project/a.txt"), file("/project/b.txt")));
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.mark(Path.of("/project/a.txt"));
            var visible = model.getVisibleEntries();

            assertTrue(visible.get(0).isMarked());
            assertFalse(visible.get(1).isMarked());
        }

        @Test
        void setRootDirectoryでマークがクリアされる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/project"), Lists.immutable.of(file("/project/a.txt")));
            entries.put(Path.of("/other"), Lists.immutable.empty());
            var model = new TreeDiredModel(Path.of("/project"), stubLister(entries));

            model.mark(Path.of("/project/a.txt"));
            model.setRootDirectory(Path.of("/other"));

            assertFalse(model.isMarked(Path.of("/project/a.txt")));
            assertTrue(model.getMarkedPaths().isEmpty());
        }
    }

    @Nested
    class depth {

        @Test
        void ネストしたエントリのdepthが正しい() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(dir("/p/a")));
            entries.put(Path.of("/p/a"), Lists.immutable.of(dir("/p/a/b")));
            entries.put(Path.of("/p/a/b"), Lists.immutable.of(file("/p/a/b/c.txt")));
            var model = new TreeDiredModel(Path.of("/p"), stubLister(entries));

            model.toggle(Path.of("/p/a"));
            model.toggle(Path.of("/p/a/b"));
            var visible = model.getVisibleEntries();

            assertEquals(0, visible.get(0).depth()); // /p/a
            assertEquals(1, visible.get(1).depth()); // /p/a/b
            assertEquals(2, visible.get(2).depth()); // /p/a/b/c.txt
        }
    }
}
