package io.github.shomah4a.alle.app;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.command.BackwardCharCommand;
import io.github.shomah4a.alle.core.command.BackwardDeleteCharCommand;
import io.github.shomah4a.alle.core.command.BeginningOfLineCommand;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.DeleteCharCommand;
import io.github.shomah4a.alle.core.command.EndOfLineCommand;
import io.github.shomah4a.alle.core.command.FindFileCommand;
import io.github.shomah4a.alle.core.command.ForwardCharCommand;
import io.github.shomah4a.alle.core.command.KillLineCommand;
import io.github.shomah4a.alle.core.command.NewlineCommand;
import io.github.shomah4a.alle.core.command.NextLineCommand;
import io.github.shomah4a.alle.core.command.PreviousLineCommand;
import io.github.shomah4a.alle.core.command.SaveBufferCommand;
import io.github.shomah4a.alle.core.command.SelfInsertCommand;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.tui.MinibufferInputPrompter;
import io.github.shomah4a.alle.tui.QuitCommand;
import io.github.shomah4a.alle.tui.ScreenRenderer;
import io.github.shomah4a.alle.tui.TerminalInputSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Alleエディタのエントリポイント。
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws IOException {
        // C-s/C-q がフロー制御（XON/XOFF）に奪われるのを防ぐ
        disableFlowControl();

        Terminal terminal = new UnixTerminal(System.in, System.out, StandardCharsets.UTF_8);
        Screen screen = new TerminalScreen(terminal);
        try {
            screen.startScreen();
            run(screen);
        } finally {
            screen.stopScreen();
            restoreFlowControl();
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
        var bufferIO = new BufferIO(
                source -> new BufferedReader(Files.newBufferedReader(Path.of(source), StandardCharsets.UTF_8)),
                destination ->
                        new BufferedWriter(Files.newBufferedWriter(Path.of(destination), StandardCharsets.UTF_8)));
        var registry = createCommandRegistry(inputSource, bufferIO);
        var keymap = createKeymap(registry);
        var resolver = new KeyResolver();
        resolver.addKeymap(keymap);

        var prompter = new MinibufferInputPrompter(frame);
        var commandLoop = new CommandLoop(inputSource, resolver, frame, bufferManager, prompter);
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

    private static CommandRegistry createCommandRegistry(TerminalInputSource inputSource, BufferIO bufferIO) {
        var registry = new CommandRegistry();
        registry.register(new SelfInsertCommand());
        registry.register(new ForwardCharCommand());
        registry.register(new BackwardCharCommand());
        registry.register(new BeginningOfLineCommand());
        registry.register(new EndOfLineCommand());
        registry.register(new DeleteCharCommand());
        registry.register(new KillLineCommand());
        registry.register(new BackwardDeleteCharCommand());
        registry.register(new NewlineCommand());
        registry.register(new NextLineCommand());
        registry.register(new PreviousLineCommand());
        registry.register(new QuitCommand(inputSource));
        registry.register(new FindFileCommand(bufferIO));
        registry.register(new SaveBufferCommand(bufferIO));
        return registry;
    }

    private static Keymap createKeymap(CommandRegistry registry) {
        var keymap = new Keymap("global");

        keymap.setDefaultCommand(registry.lookup("self-insert-command").orElseThrow());

        keymap.bind(KeyStroke.ctrl('f'), registry.lookup("forward-char").orElseThrow());
        keymap.bind(KeyStroke.ctrl('b'), registry.lookup("backward-char").orElseThrow());
        keymap.bind(KeyStroke.ctrl('a'), registry.lookup("beginning-of-line").orElseThrow());
        keymap.bind(KeyStroke.ctrl('e'), registry.lookup("end-of-line").orElseThrow());
        keymap.bind(KeyStroke.ctrl('n'), registry.lookup("next-line").orElseThrow());
        keymap.bind(KeyStroke.ctrl('p'), registry.lookup("previous-line").orElseThrow());
        keymap.bind(KeyStroke.ctrl('d'), registry.lookup("delete-char").orElseThrow());
        keymap.bind(KeyStroke.ctrl('k'), registry.lookup("kill-line").orElseThrow());
        keymap.bind(KeyStroke.of(0x7F), registry.lookup("backward-delete-char").orElseThrow());
        keymap.bind(KeyStroke.of('\n'), registry.lookup("newline").orElseThrow());
        keymap.bind(KeyStroke.ctrl('q'), registry.lookup("quit").orElseThrow());

        // C-x プレフィックスキーマップ
        var ctrlXMap = new Keymap("C-x");
        ctrlXMap.bind(KeyStroke.ctrl('c'), registry.lookup("quit").orElseThrow());
        ctrlXMap.bind(KeyStroke.ctrl('f'), registry.lookup("find-file").orElseThrow());
        ctrlXMap.bind(KeyStroke.ctrl('s'), registry.lookup("save-buffer").orElseThrow());
        keymap.bindPrefix(KeyStroke.ctrl('x'), ctrlXMap);

        return keymap;
    }

    private static void disableFlowControl() throws IOException {
        new ProcessBuilder("stty", "-ixon").inheritIO().start().onExit().join();
    }

    private static void restoreFlowControl() throws IOException {
        new ProcessBuilder("stty", "ixon").inheritIO().start().onExit().join();
    }
}
