package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.window.Frame;
import java.util.Optional;

/**
 * コマンド実行時のコンテキスト。
 * 編集操作はframe経由のactiveWindowを通じて行い、
 * バッファの作成・削除・一覧取得はbufferManagerを通じて行う。
 * triggeringKeyはコマンドを発動したキーストローク（プログラム的呼び出し時はempty）。
 */
public record CommandContext(Frame frame, BufferManager bufferManager, Optional<KeyStroke> triggeringKey) {

    /**
     * triggeringKeyなしのコンテキストを生成する。
     */
    public CommandContext(Frame frame, BufferManager bufferManager) {
        this(frame, bufferManager, Optional.empty());
    }
}
