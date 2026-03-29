package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.input.FileAttributes;
import java.nio.file.Path;

/**
 * ツリー表示の1行分のエントリを表す。
 *
 * @param path エントリのフルパス
 * @param depth ツリーの深さ（ルート直下が0）
 * @param isDirectory ディレクトリかどうか
 * @param isExpanded 展開済みかどうか（ファイルの場合はfalse）
 * @param isMarked マーク済みかどうか
 * @param attributes ファイル属性
 */
public record TreeDiredEntry(
        Path path, int depth, boolean isDirectory, boolean isExpanded, boolean isMarked, FileAttributes attributes) {}
