package io.github.shomah4a.alle.core.window;

/**
 * 垂直分割時のセパレータ位置。
 *
 * @param column セパレータのカラム位置
 * @param top    セパレータの開始行
 * @param height セパレータの高さ（行数）
 */
public record Separator(int column, int top, int height) {}
