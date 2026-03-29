package io.github.shomah4a.alle.core.input;

import java.nio.file.Path;

/**
 * ディレクトリ内のエントリを表す。
 * ファイルとディレクトリの区別を型で表現する。
 */
public sealed interface DirectoryEntry {

    /**
     * エントリのフルパスを返す。
     */
    Path path();

    /**
     * 通常ファイル。
     */
    record File(Path path) implements DirectoryEntry {}

    /**
     * ディレクトリ。
     */
    record Directory(Path path) implements DirectoryEntry {}
}
