package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Test;

class FilePathCompleterTest {

    private DirectoryLister stubLister(String... entries) {
        MutableList<DirectoryEntry> result = Lists.mutable.empty();
        for (String entry : entries) {
            if (entry.endsWith("/")) {
                result.add(new DirectoryEntry.Directory(Path.of(entry.substring(0, entry.length() - 1))));
            } else {
                result.add(new DirectoryEntry.File(Path.of(entry)));
            }
        }
        return directory -> result.toImmutable();
    }

    @Test
    void 親ディレクトリ内の前方一致するエントリを返す() {
        var completer = new FilePathCompleter(stubLister("/tmp/foo.txt", "/tmp/foobar.txt", "/tmp/bar.txt"));
        var result = completer.complete("/tmp/foo");

        assertEquals(2, result.size());
        assertTrue(result.anySatisfy(c -> c.value().equals("/tmp/foo.txt")));
        assertTrue(result.anySatisfy(c -> c.value().equals("/tmp/foobar.txt")));
    }

    @Test
    void 一致するエントリがない場合は空リストを返す() {
        var completer = new FilePathCompleter(stubLister("/tmp/bar.txt"));
        var result = completer.complete("/tmp/foo");

        assertTrue(result.isEmpty());
    }

    @Test
    void 空文字列入力では空リストを返す() {
        var completer = new FilePathCompleter(stubLister("/tmp/foo.txt"));
        var result = completer.complete("");

        assertTrue(result.isEmpty());
    }

    @Test
    void ディレクトリ一覧取得に失敗しても空リストを返す() {
        DirectoryLister failingLister = directory -> {
            throw new IOException("read error");
        };
        var completer = new FilePathCompleter(failingLister);
        var result = completer.complete("/tmp/foo");

        assertTrue(result.isEmpty());
    }

    @Test
    void ディレクトリエントリの末尾スラッシュが保持されpartialとして返る() {
        var completer = new FilePathCompleter(stubLister("/tmp/subdir/"));
        var result = completer.complete("/tmp/sub");

        assertEquals(1, result.size());
        assertEquals("/tmp/subdir/", result.get(0).value());
        assertFalse(result.get(0).terminal());
    }

    @Test
    void ファイルエントリはterminalとして返る() {
        var completer = new FilePathCompleter(stubLister("/tmp/foo.txt"));
        var result = completer.complete("/tmp/foo");

        assertEquals(1, result.size());
        assertTrue(result.get(0).terminal());
    }

    @Test
    void ファイルのラベルはファイル名のみである() {
        var completer = new FilePathCompleter(stubLister("/tmp/foo.txt", "/tmp/bar.txt"));
        var result = completer.complete("/tmp/");

        assertEquals(
                "foo.txt", result.detect(c -> c.value().equals("/tmp/foo.txt")).label());
        assertEquals(
                "bar.txt", result.detect(c -> c.value().equals("/tmp/bar.txt")).label());
    }

    @Test
    void ディレクトリのラベルはディレクトリ名に末尾スラッシュ付きである() {
        var completer = new FilePathCompleter(stubLister("/tmp/subdir/"));
        var result = completer.complete("/tmp/sub");

        assertEquals("subdir/", result.get(0).label());
    }

    @Test
    void 末尾スラッシュの入力ではそのディレクトリの中身を一覧する() {
        var completer = new FilePathCompleter(stubLister("/tmp/subdir/file1.txt", "/tmp/subdir/file2.txt"));
        var result = completer.complete("/tmp/subdir/");

        assertEquals(2, result.size());
        assertTrue(result.anySatisfy(c -> c.value().equals("/tmp/subdir/file1.txt")));
        assertTrue(result.anySatisfy(c -> c.value().equals("/tmp/subdir/file2.txt")));
    }

    @Test
    void ルートディレクトリの入力ではルートの中身を一覧する() {
        var completer = new FilePathCompleter(stubLister("/tmp/", "/home/"));
        var result = completer.complete("/");

        assertEquals(2, result.size());
        assertTrue(result.anySatisfy(c -> c.value().equals("/tmp/")));
        assertTrue(result.anySatisfy(c -> c.value().equals("/home/")));
    }

    @Test
    void 末尾スラッシュの入力で存在しないディレクトリは空リストを返す() {
        DirectoryLister failingLister = directory -> {
            throw new IOException("no such directory");
        };
        var completer = new FilePathCompleter(failingLister);
        var result = completer.complete("/tmp/nonexistent/");

        assertTrue(result.isEmpty());
    }
}
