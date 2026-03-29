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
     * ファイル属性を返す。
     */
    FileAttributes attributes();

    /**
     * 通常ファイル。
     */
    record File(Path path, FileAttributes attributes) implements DirectoryEntry {}

    /**
     * ディレクトリ。
     */
    record Directory(Path path, FileAttributes attributes) implements DirectoryEntry {}
}
