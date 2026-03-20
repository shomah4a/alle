package io.github.shomah4a.alle.app;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
import io.github.shomah4a.alle.core.EditorCore;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.script.EditorFacade;
import io.github.shomah4a.alle.script.EvalExpressionCommand;
import io.github.shomah4a.alle.script.graalpy.GraalPyEngine;
import io.github.shomah4a.alle.script.graalpy.GraalPyEngineFactory;
import io.github.shomah4a.alle.tui.EditorRunner;
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
        var inputSource = new TerminalInputSource(screen);
        var bufferIO = new BufferIO(
                source -> new BufferedReader(Files.newBufferedReader(Path.of(source), StandardCharsets.UTF_8)),
                destination ->
                        new BufferedWriter(Files.newBufferedWriter(Path.of(destination), StandardCharsets.UTF_8)));
        DirectoryLister directoryLister = Main::listDirectory;

        var core = EditorCore.create(inputSource, MinibufferInputPrompter::new, bufferIO, directoryLister, inputSource);

        // TUI固有コマンドの追加
        core.commandRegistry().register(new QuitCommand(inputSource));
        core.keymap()
                .bind(KeyStroke.ctrl('q'), core.commandRegistry().lookup("quit").orElseThrow());

        // スクリプトエンジンの初期化
        var scriptEngineFactory = new GraalPyEngineFactory();
        var scriptEngine = (GraalPyEngine) scriptEngineFactory.create();
        var editorFacade = new EditorFacade(core.frame(), core.bufferManager(), core.messageBuffer());
        scriptEngine.initAlleModule(editorFacade);

        // eval-expression コマンド (M-:)
        core.commandRegistry().register(new EvalExpressionCommand(scriptEngine));
        core.keymap()
                .bind(
                        KeyStroke.meta(':'),
                        core.commandRegistry().lookup("eval-expression").orElseThrow());

        var renderer = new ScreenRenderer(screen, core.messageBuffer());
        var runner = new EditorRunner(inputSource, screen, renderer, core.commandLoop(), core.frame());

        try {
            runner.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scriptEngine.close();
        }
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
