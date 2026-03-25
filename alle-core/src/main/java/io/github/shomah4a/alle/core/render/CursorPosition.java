package io.github.shomah4a.alle.core.render;

/**
 * 画面上のカーソル位置を表す。Lanterna等のTUIライブラリに依存しない汎用的な座標表現。
 *
 * @param column 0始まりのカラム位置
 * @param row 0始まりの行位置
 */
public record CursorPosition(int column, int row) {}
