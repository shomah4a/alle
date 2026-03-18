package io.github.shomah4a.alle.app;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.BackwardCharCommand;
import io.github.shomah4a.alle.core.command.BackwardDeleteCharCommand;
import io.github.shomah4a.alle.core.command.BeginningOfLineCommand;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CopyRegionCommand;
import io.github.shomah4a.alle.core.command.DeleteCharCommand;
import io.github.shomah4a.alle.core.command.DeleteOtherWindowsCommand;
import io.github.shomah4a.alle.core.command.DeleteWindowCommand;
import io.github.shomah4a.alle.core.command.EndOfLineCommand;
import io.github.shomah4a.alle.core.command.ExecuteCommandCommand;
import io.github.shomah4a.alle.core.command.FindFileCommand;
import io.github.shomah4a.alle.core.command.ForwardCharCommand;
import io.github.shomah4a.alle.core.command.KeyboardQuitCommand;
import io.github.shomah4a.alle.core.command.KillBufferCommand;
import io.github.shomah4a.alle.core.command.KillLineCommand;
import io.github.shomah4a.alle.core.command.KillRegionCommand;
import io.github.shomah4a.alle.core.command.NewlineCommand;
import io.github.shomah4a.alle.core.command.NextLineCommand;
import io.github.shomah4a.alle.core.command.OtherWindowCommand;
import io.github.shomah4a.alle.core.command.PreviousLineCommand;
import io.github.shomah4a.alle.core.command.RedoCommand;
import io.github.shomah4a.alle.core.command.SaveBufferCommand;
import io.github.shomah4a.alle.core.command.SaveBuffersKillAlleCommand;
import io.github.shomah4a.alle.core.command.SelfInsertCommand;
import io.github.shomah4a.alle.core.command.SetMarkCommand;
import io.github.shomah4a.alle.core.command.ShutdownHandler;
import io.github.shomah4a.alle.core.command.SplitWindowBelowCommand;
import io.github.shomah4a.alle.core.command.SplitWindowRightCommand;
import io.github.shomah4a.alle.core.command.SwitchBufferCommand;
import io.github.shomah4a.alle.core.command.UndoCommand;
import io.github.shomah4a.alle.core.command.YankCommand;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.MarkdownMode;
import io.github.shomah4a.alle.core.mode.TextMode;
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
import org.eclipse.collections.api.list.ListIterable;

/**
 * Alleエディタのエントリポイント。
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws IOException {
        // C-s/C-q がフロー制御（XON/XOFF）に奪われるのを防ぐ
        disableFlowControl();

        var terminal = new UnixTerminal(System.in, System.out, StandardCharsets.UTF_8);
        // ESC単独入力後に次のキーを待つ猶予（4 × 250ms = 1秒）
        // ESC + x で M-x として解釈するために必要
        terminal.getInputDecoder().setTimeoutUnits(4);
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
        var buffer = new EditableBuffer("*scratch*", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);
        var messageBuffer = new MessageBuffer("*Messages*", 1000);
        var warningBuffer = new MessageBuffer("*Warnings*", 1000);
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        bufferManager.add(messageBuffer);
        bufferManager.add(warningBuffer);

        var inputSource = new TerminalInputSource(screen);
        var bufferIO = new BufferIO(
                source -> new BufferedReader(Files.newBufferedReader(Path.of(source), StandardCharsets.UTF_8)),
                destination ->
                        new BufferedWriter(Files.newBufferedWriter(Path.of(destination), StandardCharsets.UTF_8)));
        DirectoryLister directoryLister = Main::listDirectory;
        var autoModeMap = new AutoModeMap(TextMode::new);
        autoModeMap.register("md", MarkdownMode::new);
        autoModeMap.register("markdown", MarkdownMode::new);
        var shutdownHandler = new ShutdownHandler();
        var registry = createCommandRegistry(inputSource, bufferIO, directoryLister, autoModeMap, shutdownHandler);
        var keymap = createKeymap(registry);
        var resolver = new KeyResolver();
        resolver.addKeymap(keymap);

        var prompter = new MinibufferInputPrompter(frame);
        var killRing = new io.github.shomah4a.alle.core.command.KillRing();
        var commandLoop = new CommandLoop(
                inputSource, resolver, frame, bufferManager, prompter, killRing, messageBuffer, warningBuffer);
        var renderer = new ScreenRenderer(screen, messageBuffer);

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

    private static CommandRegistry createCommandRegistry(
            TerminalInputSource inputSource,
            BufferIO bufferIO,
            DirectoryLister directoryLister,
            AutoModeMap autoModeMap,
            ShutdownHandler shutdownHandler) {
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
        registry.register(
                new FindFileCommand(bufferIO, directoryLister, Path.of("").toAbsolutePath(), autoModeMap));
        registry.register(new SaveBufferCommand(bufferIO, directoryLister));
        registry.register(new SwitchBufferCommand());
        registry.register(new OtherWindowCommand());
        registry.register(new KillBufferCommand());
        registry.register(new ExecuteCommandCommand(registry));
        registry.register(new SetMarkCommand());
        registry.register(new KillRegionCommand());
        registry.register(new CopyRegionCommand());
        registry.register(new YankCommand());
        registry.register(new UndoCommand());
        registry.register(new RedoCommand());
        registry.register(new SplitWindowBelowCommand());
        registry.register(new SplitWindowRightCommand());
        registry.register(new DeleteWindowCommand());
        registry.register(new DeleteOtherWindowsCommand());
        registry.register(new KeyboardQuitCommand());
        registry.register(new SaveBuffersKillAlleCommand(shutdownHandler, inputSource));
        return registry;
    }

    private static Keymap createKeymap(CommandRegistry registry) {
        Keymap.setQuitCommand(registry.lookup("keyboard-quit").orElseThrow());
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

        // アローキー
        keymap.bind(
                KeyStroke.of(KeyStroke.ARROW_RIGHT),
                registry.lookup("forward-char").orElseThrow());
        keymap.bind(
                KeyStroke.of(KeyStroke.ARROW_LEFT),
                registry.lookup("backward-char").orElseThrow());
        keymap.bind(
                KeyStroke.of(KeyStroke.ARROW_DOWN), registry.lookup("next-line").orElseThrow());
        keymap.bind(
                KeyStroke.of(KeyStroke.ARROW_UP),
                registry.lookup("previous-line").orElseThrow());

        // C-x プレフィックスキーマップ
        var ctrlXMap = new Keymap("C-x");
        ctrlXMap.bind(
                KeyStroke.ctrl('c'), registry.lookup("save-buffers-kill-alle").orElseThrow());
        ctrlXMap.bind(KeyStroke.ctrl('f'), registry.lookup("find-file").orElseThrow());
        ctrlXMap.bind(KeyStroke.ctrl('s'), registry.lookup("save-buffer").orElseThrow());
        ctrlXMap.bind(KeyStroke.of('o'), registry.lookup("other-window").orElseThrow());
        ctrlXMap.bind(KeyStroke.of('b'), registry.lookup("switch-to-buffer").orElseThrow());
        ctrlXMap.bind(KeyStroke.of('k'), registry.lookup("kill-buffer").orElseThrow());
        ctrlXMap.bind(KeyStroke.of('1'), registry.lookup("delete-other-windows").orElseThrow());
        ctrlXMap.bind(KeyStroke.of('2'), registry.lookup("split-window-below").orElseThrow());
        ctrlXMap.bind(KeyStroke.of('3'), registry.lookup("split-window-right").orElseThrow());
        ctrlXMap.bind(KeyStroke.of('0'), registry.lookup("delete-window").orElseThrow());
        keymap.bindPrefix(KeyStroke.ctrl('x'), ctrlXMap);

        // C-SPC (mark)
        keymap.bind(KeyStroke.ctrl(' '), registry.lookup("set-mark").orElseThrow());

        // kill/yank
        keymap.bind(KeyStroke.ctrl('w'), registry.lookup("kill-region").orElseThrow());
        keymap.bind(KeyStroke.ctrl('y'), registry.lookup("yank").orElseThrow());

        // M-w (copy region)
        keymap.bind(KeyStroke.meta('w'), registry.lookup("copy-region").orElseThrow());

        // C-/ (undo) — ターミナルでは0x1FとしてLanternaがCtrl+'_'に変換する
        keymap.bind(KeyStroke.ctrl('_'), registry.lookup("undo").orElseThrow());

        // M-/ (redo)
        keymap.bind(KeyStroke.meta('/'), registry.lookup("redo").orElseThrow());

        // M-x
        keymap.bind(KeyStroke.meta('x'), registry.lookup("execute-command").orElseThrow());

        return keymap;
    }

    private static ListIterable<String> listDirectory(Path directory) throws IOException {
        var entries = org.eclipse.collections.api.factory.Lists.mutable.<String>empty();
        try (var stream = Files.list(directory)) {
            stream.forEach(entry -> {
                String name = entry.toString();
                if (Files.isDirectory(entry)) {
                    entries.add(name + "/");
                } else {
                    entries.add(name);
                }
            });
        }
        return entries;
    }

    private static void disableFlowControl() throws IOException {
        new ProcessBuilder("stty", "-ixon").inheritIO().start().onExit().join();
    }

    private static void restoreFlowControl() throws IOException {
        new ProcessBuilder("stty", "ixon").inheritIO().start().onExit().join();
    }
}
