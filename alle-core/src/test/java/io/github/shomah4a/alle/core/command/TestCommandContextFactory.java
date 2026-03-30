package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * テスト用のCommandContext生成ヘルパー。
 */
final class TestCommandContextFactory {

    private static final InputPrompter NOOP_PROMPTER =
            (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());

    private static final SettingsRegistry SETTINGS_REGISTRY = new SettingsRegistry();

    private TestCommandContextFactory() {}

    /**
     * テスト用のSettingsRegistryを返す。
     */
    static SettingsRegistry settingsRegistry() {
        return SETTINGS_REGISTRY;
    }

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
        return new CommandContext(
                frame,
                bufferManager,
                frame.getActiveWindow(),
                inputPrompter,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, SETTINGS_REGISTRY),
                new MessageBuffer("*Warnings*", 100, SETTINGS_REGISTRY),
                SETTINGS_REGISTRY,
                new CommandResolver(new CommandRegistry()));
    }

    /**
     * triggeringKey付きのコンテキストを生成する。コマンド履歴なし。
     */
    static CommandContext create(Frame frame, BufferManager bufferManager, KeyStroke triggeringKey) {
        return new CommandContext(
                frame,
                bufferManager,
                frame.getActiveWindow(),
                NOOP_PROMPTER,
                Optional.of(triggeringKey),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, SETTINGS_REGISTRY),
                new MessageBuffer("*Warnings*", 100, SETTINGS_REGISTRY),
                SETTINGS_REGISTRY,
                new CommandResolver(new CommandRegistry()));
    }

    /**
     * KillRingとlastCommand指定でコンテキストを生成する。
     */
    static CommandContext create(
            Frame frame, BufferManager bufferManager, KillRing killRing, Optional<String> lastCommand) {
        return new CommandContext(
                frame,
                bufferManager,
                frame.getActiveWindow(),
                NOOP_PROMPTER,
                Optional.empty(),
                Optional.empty(),
                lastCommand,
                killRing,
                new MessageBuffer("*Messages*", 100, SETTINGS_REGISTRY),
                new MessageBuffer("*Warnings*", 100, SETTINGS_REGISTRY),
                SETTINGS_REGISTRY,
                new CommandResolver(new CommandRegistry()));
    }

    /**
     * デフォルトのバッファ・ウィンドウ・フレームでコンテキストを生成する。
     */
    static CommandContext createDefault() {
        var bufferFacade = new BufferFacade(new TextBuffer("test", new GapTextModel(), SETTINGS_REGISTRY));
        var window = new Window(bufferFacade);
        var minibuffer =
                new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS_REGISTRY)));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(bufferFacade);
        return create(frame, bufferManager);
    }
}
