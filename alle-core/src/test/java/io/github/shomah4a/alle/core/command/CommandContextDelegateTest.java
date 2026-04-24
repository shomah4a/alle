package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.io.PathOpenService;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommandContextDelegateTest {

    private static final SettingsRegistry SETTINGS = new SettingsRegistry();
    private static final InputPrompter NOOP_PROMPTER =
            (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());

    private Frame frame;
    private BufferManager bufferManager;
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), SETTINGS));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
        registry = new CommandRegistry();
    }

    private CommandContext createContext(Optional<String> thisCommand, Optional<String> lastCommand) {
        return new CommandContext(
                frame,
                bufferManager,
                frame.getActiveWindow(),
                NOOP_PROMPTER,
                Lists.immutable.empty(),
                thisCommand,
                lastCommand,
                new KillRing(),
                new MessageBuffer("*Messages*", 100, SETTINGS),
                new MessageBuffer("*Warnings*", 100, SETTINGS),
                SETTINGS,
                new CommandResolver(registry),
                new NoOpOverridingKeymapController(),
                new PathOpenService(
                        new BufferIO(
                                source -> {
                                    throw new java.io.IOException("stub");
                                },
                                destination -> {
                                    throw new java.io.IOException("stub");
                                },
                                SETTINGS),
                        new AutoModeMap(TextMode::new),
                        new ModeRegistry(),
                        SETTINGS,
                        path -> false,
                        (pathString, bm, f) -> {}));
    }

    @Nested
    class delegateメソッド {

        @Test
        void 登録済みコマンドを名前で実行できる() {
            var executed = new AtomicBoolean(false);
            registry.register(new Command() {
                @Override
                public String name() {
                    return "target-command";
                }

                @Override
                public CompletableFuture<Void> execute(CommandContext context) {
                    executed.set(true);
                    return CompletableFuture.completedFuture(null);
                }
            });

            var ctx = createContext(Optional.of("caller-command"), Optional.of("previous-command"));
            ctx.delegate("target-command").join();

            assertTrue(executed.get());
        }

        @Test
        void delegate先にはthisCommandが元のまま渡される() {
            var receivedThisCommand = new AtomicReference<Optional<String>>();
            registry.register(new Command() {
                @Override
                public String name() {
                    return "target-command";
                }

                @Override
                public CompletableFuture<Void> execute(CommandContext context) {
                    receivedThisCommand.set(context.thisCommand());
                    return CompletableFuture.completedFuture(null);
                }
            });

            var ctx = createContext(Optional.of("caller-command"), Optional.of("previous-command"));
            ctx.delegate("target-command").join();

            assertEquals(Optional.of("caller-command"), receivedThisCommand.get());
        }

        @Test
        void delegate先にはlastCommandが元のまま渡される() {
            var receivedLastCommand = new AtomicReference<Optional<String>>();
            registry.register(new Command() {
                @Override
                public String name() {
                    return "target-command";
                }

                @Override
                public CompletableFuture<Void> execute(CommandContext context) {
                    receivedLastCommand.set(context.lastCommand());
                    return CompletableFuture.completedFuture(null);
                }
            });

            var ctx = createContext(Optional.of("caller-command"), Optional.of("previous-command"));
            ctx.delegate("target-command").join();

            assertEquals(Optional.of("previous-command"), receivedLastCommand.get());
        }

        @Test
        void 未登録のコマンド名を指定するとIllegalArgumentExceptionが発生する() {
            var ctx = createContext(Optional.empty(), Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> ctx.delegate("nonexistent-command"));
        }
    }
}
