package io.github.shomah4a.alle.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.command.NoOpOverridingKeymapController;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScriptCommandContextTest {

    private static final SettingsRegistry SETTINGS = new SettingsRegistry();

    private Frame frame;
    private BufferManager bufferManager;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), SETTINGS));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
    }

    private CommandContext createCommandContext(org.eclipse.collections.api.list.ListIterable<KeyStroke> keySequence) {
        return new CommandContext(
                frame,
                bufferManager,
                frame.getActiveWindow(),
                (msg, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled()),
                keySequence,
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, SETTINGS),
                new MessageBuffer("*Warnings*", 100, SETTINGS),
                SETTINGS,
                new CommandResolver(new CommandRegistry()),
                new NoOpOverridingKeymapController());
    }

    @Nested
    class triggeringKeySequence {

        @Test
        void 空のキーシーケンスを返す() {
            var ctx = createCommandContext(Lists.immutable.empty());
            var scriptCtx = ScriptCommandContext.of(ctx);

            var result = scriptCtx.triggeringKeySequence();

            assertTrue(result.isEmpty());
        }

        @Test
        void 単一キーのシーケンスを返す() {
            var key = KeyStroke.of('a');
            var ctx = createCommandContext(Lists.immutable.of(key));
            var scriptCtx = ScriptCommandContext.of(ctx);

            var result = scriptCtx.triggeringKeySequence();

            assertEquals(1, result.size());
            assertEquals(key, result.get(0));
        }

        @Test
        void プレフィックスキーシーケンスを返す() {
            var ctrlX = KeyStroke.ctrl('x');
            var ctrlF = KeyStroke.ctrl('f');
            var ctx = createCommandContext(Lists.immutable.of(ctrlX, ctrlF));
            var scriptCtx = ScriptCommandContext.of(ctx);

            var result = scriptCtx.triggeringKeySequence();

            assertEquals(2, result.size());
            assertEquals(ctrlX, result.get(0));
            assertEquals(ctrlF, result.get(1));
        }
    }
}
