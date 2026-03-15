package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class FilePathCompleterTest {

    private DirectoryLister stubLister(String... entries) {
        return directory -> Lists.immutable.of(entries);
    }

    @Test
    void 親ディレクトリ内の前方一致するエントリを返す() {
        var completer = new FilePathCompleter(stubLister("/tmp/foo.txt", "/tmp/foobar.txt", "/tmp/bar.txt"));
        var result = completer.complete("/tmp/foo");

        assertEquals(2, result.size());
        assertTrue(result.contains("/tmp/foo.txt"));
        assertTrue(result.contains("/tmp/foobar.txt"));
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
    void ディレクトリエントリの末尾スラッシュが保持される() {
        var completer = new FilePathCompleter(stubLister("/tmp/subdir/"));
        var result = completer.complete("/tmp/sub");

        assertEquals(1, result.size());
        assertEquals("/tmp/subdir/", result.get(0));
    }
}
