package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.FrameActor;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowActor;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * テスト用のCommandContext生成ヘルパー。
 */
final class TestCommandContextFactory {

    private static final InputPrompter NOOP_PROMPTER =
            (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());

    private TestCommandContextFactory() {}

    /**
     * 最小限のコンテキストを生成する。triggeringKey・コマンド履歴なし。
     */
    static CommandContext create(Frame frame, BufferManager bufferManager) {
        return create(frame, bufferManager, NOOP_PROMPTER);
    }

    /**
     * InputPrompter指定でコンテキストを生成する。triggeringKey・コマンド履歴なし。
     */
    static CommandContext create(Frame frame, BufferManager bufferManager, InputPrompter inputPrompter) {
        var windowActor = new WindowActor(frame.getActiveWindow());
        var frameActor = new FrameActor(frame);
        return new CommandContext(
                frame,
                frameActor,
                bufferManager,
                windowActor,
                inputPrompter,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100),
                new MessageBuffer("*Warnings*", 100));
    }

    /**
     * triggeringKey付きのコンテキストを生成する。コマンド履歴なし。
     */
    static CommandContext create(Frame frame, BufferManager bufferManager, KeyStroke triggeringKey) {
        var windowActor = new WindowActor(frame.getActiveWindow());
        var frameActor = new FrameActor(frame);
        return new CommandContext(
                frame,
                frameActor,
                bufferManager,
                windowActor,
                NOOP_PROMPTER,
                Optional.of(triggeringKey),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100),
                new MessageBuffer("*Warnings*", 100));
    }

    /**
     * KillRingとlastCommand指定でコンテキストを生成する。
     */
    static CommandContext create(
            Frame frame, BufferManager bufferManager, KillRing killRing, Optional<String> lastCommand) {
        var windowActor = new WindowActor(frame.getActiveWindow());
        var frameActor = new FrameActor(frame);
        return new CommandContext(
                frame,
                frameActor,
                bufferManager,
                windowActor,
                NOOP_PROMPTER,
                Optional.empty(),
                Optional.empty(),
                lastCommand,
                killRing,
                new MessageBuffer("*Messages*", 100),
                new MessageBuffer("*Warnings*", 100));
    }

    /**
     * デフォルトのバッファ・ウィンドウ・フレームでコンテキストを生成する。
     */
    static CommandContext createDefault() {
        var buffer = new EditableBuffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        return create(frame, bufferManager);
    }
}
