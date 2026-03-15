package io.github.shomah4a.alle.core.window;

/**
 * 画面上の矩形領域。
 *
 * @param top    上端の行番号
 * @param left   左端のカラム番号
 * @param width  幅（カラム数）
 * @param height 高さ（行数）
 */
public record Rect(int top, int left, int width, int height) {}
