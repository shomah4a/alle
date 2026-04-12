package io.github.shomah4a.alle.core.statusline;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.window.Window;

/**
 * ステータスライン描画時のコンテキスト。
 * 読み取り専用の情報のみを保持し、描画に必要な状態を提供する。
 */
public record StatusLineContext(Window window, BufferFacade buffer) {}
