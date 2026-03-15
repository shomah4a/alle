package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowActor;
import java.util.Optional;

/**
 * テスト用のCommandContext生成ヘルパー。
 */
final class TestCommandContextFactory {

    private TestCommandContextFactory() {}

    /**
     * 最小限のコンテキストを生成する。triggeringKey・コマンド履歴なし。
     */
    static CommandContext create(Frame frame, BufferManager bufferManager) {
        var windowActor = new WindowActor(frame.getActiveWindow());
        return new CommandContext(
                frame, bufferManager, windowActor, Optional.empty(), Optional.empty(), Optional.empty());
    }

    /**
     * triggeringKey付きのコンテキストを生成する。コマンド履歴なし。
     */
    static CommandContext create(Frame frame, BufferManager bufferManager, KeyStroke triggeringKey) {
        var windowActor = new WindowActor(frame.getActiveWindow());
        return new CommandContext(
                frame, bufferManager, windowActor, Optional.of(triggeringKey), Optional.empty(), Optional.empty());
    }

    /**
     * デフォルトのバッファ・ウィンドウ・フレームでコンテキストを生成する。
     */
    static CommandContext createDefault() {
        var buffer = new Buffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new Buffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return create(frame, bufferManager);
    }
}
