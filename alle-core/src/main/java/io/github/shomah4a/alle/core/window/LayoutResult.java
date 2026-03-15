package io.github.shomah4a.alle.core.window;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;

/**
 * WindowLayoutの計算結果。
 * 各ウィンドウの矩形とセパレータ位置を保持する。
 *
 * @param windowRects  ウィンドウから矩形へのマッピング
 * @param separators   垂直分割のセパレータ位置リスト
 */
public record LayoutResult(ImmutableMap<Window, Rect> windowRects, ListIterable<Separator> separators) {}
