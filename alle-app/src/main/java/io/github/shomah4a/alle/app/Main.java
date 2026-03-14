package io.github.shomah4a.alle.app;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.DefaultTerminalFactory;
import com.googlecode.lanterna.terminal.Terminal;
import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.command.BackwardCharCommand;
import io.github.shomah4a.alle.core.command.BackwardDeleteCharCommand;
import io.github.shomah4a.alle.core.command.BeginningOfLineCommand;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.command.DeleteCharCommand;
import io.github.shomah4a.alle.core.command.EndOfLineCommand;
import io.github.shomah4a.alle.core.command.ForwardCharCommand;
import io.github.shomah4a.alle.core.command.KillLineCommand;
import io.github.shomah4a.alle.core.command.NewlineCommand;
import io.github.shomah4a.alle.core.command.SelfInsertCommand;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.tui.QuitCommand;
import io.github.shomah4a.alle.tui.ScreenRenderer;
import io.github.shomah4a.alle.tui.TerminalInputSource;
import java.io.IOException;

/**
 * Alleエディタのエントリポイント。
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws IOException {
        try (Terminal terminal =
                new DefaultTerminalFactory().setForceTextTerminal(true).createTerminal()) {
            Screen screen = new TerminalScreen(terminal);
            try {
                screen.startScreen();
                run(screen);
            } finally {
                screen.stopScreen();
            }
        }
    }

    private static void run(Screen screen) throws IOException {
        var buffer = new Buffer("*scratch*", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new Buffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);

        var inputSource = new TerminalInputSource(screen);
        var keymap = createKeymap(inputSource);
        var resolver = new KeyResolver();
        resolver.addKeymap(keymap);

        var commandLoop = new CommandLoop(inputSource, resolver, frame, bufferManager);
        var renderer = new ScreenRenderer(screen);

        renderer.render(frame);

        while (true) {
            var keyOpt = inputSource.readKeyStroke();
            if (keyOpt.isEmpty()) {
                break;
            }
            commandLoop.processKey(keyOpt.get());
            renderer.render(frame);
        }
    }

    private static Keymap createKeymap(TerminalInputSource inputSource) {
        var keymap = new Keymap("global");

        keymap.bindPrintableAscii(new SelfInsertCommand());

        keymap.bind(KeyStroke.ctrl('f'), new ForwardCharCommand());
        keymap.bind(KeyStroke.ctrl('b'), new BackwardCharCommand());
        keymap.bind(KeyStroke.ctrl('a'), new BeginningOfLineCommand());
        keymap.bind(KeyStroke.ctrl('e'), new EndOfLineCommand());
        keymap.bind(KeyStroke.ctrl('d'), new DeleteCharCommand());
        keymap.bind(KeyStroke.ctrl('k'), new KillLineCommand());
        keymap.bind(KeyStroke.of(0x7F), new BackwardDeleteCharCommand());
        keymap.bind(KeyStroke.of('\n'), new NewlineCommand());
        keymap.bind(KeyStroke.ctrl('q'), new QuitCommand(inputSource));

        return keymap;
    }
}
