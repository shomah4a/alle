package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.window.Frame;

/**
 * コマンド実行時のコンテキスト。
 * 編集操作はframe経由のactiveWindowを通じて行い、
 * バッファの作成・削除・一覧取得はbufferManagerを通じて行う。
 */
public record CommandContext(Frame frame, BufferManager bufferManager) {}
