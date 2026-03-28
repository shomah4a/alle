package io.github.shomah4a.alle.core.styling;

/**
 * テキスト中の位置を行番号と行内UTF-8バイトオフセットで表す。
 *
 * <p>Tree-sitterのTSPointに対応する。rowは0始まりの行番号、
 * columnはその行の先頭からのUTF-8バイトオフセット。
 *
 * @param row 0始まりの行番号
 * @param column 行先頭からのUTF-8バイトオフセット
 */
public record Utf8Position(int row, int column) {}
