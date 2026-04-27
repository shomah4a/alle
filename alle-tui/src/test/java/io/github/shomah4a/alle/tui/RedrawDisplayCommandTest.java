package io.github.shomah4a.alle.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RedrawDisplayCommandTest {

    private static final SettingsRegistry SETTINGS = new SettingsRegistry();

    private AtomicBoolean flag;
    private RedrawDisplayCommand command;

    @BeforeEach
    void setUp() {
        flag = new AtomicBoolean(false);
        command = new RedrawDisplayCommand(flag);
    }

    @Test
    void コマンド名がredraw_displayである() {
        assertEquals("redraw-display", command.name());
    }

    @Test
    void リージョンを維持する() {
        assertTrue(command.keepsRegionActive());
    }

    @Test
    void 実行するとフラグがtrueになる() {
        assertFalse(flag.get());
        var unused = command.execute(createContext());
        assertTrue(flag.get());
    }

    @Test
    void フラグが既にtrueでも正常に完了する() {
        flag.set(true);
        var unused = command.execute(createContext());
        assertTrue(flag.get());
    }

    private static CommandContext createContext() {
        var buffer = new TextBuffer("*scratch*", new GapTextModel(), SETTINGS);
        var window = new Window(new BufferFacade(buffer));
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
        var frame = new Frame(window, minibuffer);
        var stubBufferIO = new BufferIO(
                source -> {
                    throw new java.io.IOException("stub");
                },
                destination -> {
                    throw new java.io.IOException("stub");
                },
                SETTINGS);
        var pathOpenService = new PathOpenService(
                stubBufferIO,
                new AutoModeMap(TextMode::new),
                new ModeRegistry(),
                SETTINGS,
                path -> false,
                (pathString, bufferManager, f) -> {});
        return new CommandContext(
                frame,
                new BufferManager(),
                window,
                (prompt, history) -> {
                    throw new UnsupportedOperationException();
                },
                Lists.immutable.empty(),
                Optional.of("redraw-display"),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, SETTINGS),
                new MessageBuffer("*Warnings*", 100, SETTINGS),
                SETTINGS,
                new CommandResolver(new CommandRegistry()),
                new NoOpOverridingKeymapController(),
                pathOpenService);
    }
}
