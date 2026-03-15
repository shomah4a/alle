package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.WindowActor;
import java.util.Optional;

/**
 * コマンド実行時のコンテキスト。
 * 編集操作はactiveWindowActor経由で行う。
 * バッファの作成・削除・一覧取得はbufferManagerを通じて行う。
 * triggeringKeyはコマンドを発動したキーストローク（プログラム的呼び出し時はempty）。
 * thisCommandは現在実行中のコマンド名、lastCommandは直前に実行されたコマンド名。
 */
public record CommandContext(
        Frame frame,
        BufferManager bufferManager,
        WindowActor activeWindowActor,
        Optional<KeyStroke> triggeringKey,
        Optional<String> thisCommand,
        Optional<String> lastCommand) {}
