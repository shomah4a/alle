package io.github.shomah4a.alle.core.window;

/**
 * ウィンドウの表示可能領域サイズ。
 * モードライン等を除いた、バッファ内容を表示できる行数と列数を表す。
 *
 * @param rows 表示可能な行数
 * @param columns 表示可能な列数
 */
public record ViewportSize(int rows, int columns) {}
