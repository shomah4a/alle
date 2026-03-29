package io.github.shomah4a.alle.app;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
import io.github.shomah4a.alle.core.EditorCore;
import io.github.shomah4a.alle.core.input.DirectoryEntry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.MinibufferInputPrompter;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.script.EditorFacade;
import io.github.shomah4a.alle.script.EvalExpressionCommand;
import io.github.shomah4a.alle.script.MessageBufferOutputStream;
import io.github.shomah4a.alle.script.graalpy.GraalPyEngineFactory;
import io.github.shomah4a.alle.tui.EditorRunner;
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

        var terminal =
                new UnixTerminal(System.in, System.out, StandardCharsets.UTF_8, UnixLikeTerminal.CtrlCBehaviour.TRAP);
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
        var settingsRegistry = new SettingsRegistry();
        settingsRegistry.register(EditorSettings.INDENT_WIDTH);
        settingsRegistry.register(EditorSettings.COMMENT_STRING);
        var bufferIO = new BufferIO(
                source -> new BufferedReader(Files.newBufferedReader(Path.of(source), StandardCharsets.UTF_8)),
                destination ->
                        new BufferedWriter(Files.newBufferedWriter(Path.of(destination), StandardCharsets.UTF_8)),
                settingsRegistry);
        DirectoryLister directoryLister = Main::listDirectory;

        var core = EditorCore.create(
                inputSource, MinibufferInputPrompter::new, bufferIO, directoryLister, inputSource, settingsRegistry);

        // スクリプトエンジンの初期化
        var msg = core.messageBuffer();
        msg.message("Initializing script engine...");
        var syntaxAnalyzerRegistry = io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry.createWithBuiltins();
        var editorFacade = new EditorFacade(
                core.frame(),
                msg,
                core.commandRegistry(),
                core.keymap(),
                core.modeRegistry(),
                core.autoModeMap(),
                syntaxAnalyzerRegistry);
        var stdoutStream =
                new MessageBufferOutputStream(core.bufferManager(), "*Python Output*", 1000, settingsRegistry);
        var stderrStream =
                new MessageBufferOutputStream(core.bufferManager(), "*Python Error*", 1000, settingsRegistry);
        var logStream = new MessageBufferOutputStream(core.bufferManager(), "*Python Log*", 1000, settingsRegistry);
        msg.message("Creating GraalPy engine...");
        var scriptEngineFactory = new GraalPyEngineFactory(editorFacade, stdoutStream, stderrStream, logStream);
        var scriptEngine = scriptEngineFactory.create();
        msg.message("Script engine initialized.");

        // ユーザー初期化スクリプトの読み込み
        loadUserInit(scriptEngine, msg, core.warningBuffer());

        // eval-expression コマンド (M-:)
        var evalHistory = new InputHistory();
        core.commandRegistry().register(new EvalExpressionCommand(scriptEngine, evalHistory));
        core.keymap()
                .bind(
                        KeyStroke.meta(':'),
                        core.commandRegistry().lookup("eval-expression").orElseThrow());

        var renderer = new ScreenRenderer(
                screen,
                new io.github.shomah4a.alle.core.styling.DefaultFaceTheme(),
                new io.github.shomah4a.alle.tui.FaceResolver());
        var runner =
                new EditorRunner(inputSource, screen, renderer, core.commandLoop(), core.frame(), core.messageBuffer());

        try {
            runner.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scriptEngine.close();
            scriptEngineFactory.close();
        }
    }

    private static ListIterable<DirectoryEntry> listDirectory(Path directory) throws IOException {
        var entries = org.eclipse.collections.api.factory.Lists.mutable.<DirectoryEntry>empty();
        try (var stream = Files.list(directory)) {
            stream.forEach(entry -> {
                var attrs = readFileAttributes(entry);
                if (Files.isDirectory(entry)) {
                    entries.add(new DirectoryEntry.Directory(entry, attrs));
                } else {
                    entries.add(new DirectoryEntry.File(entry, attrs));
                }
            });
        }
        return entries;
    }

    private static io.github.shomah4a.alle.core.input.FileAttributes readFileAttributes(Path path) {
        try {
            var posix = Files.readAttributes(path, java.nio.file.attribute.PosixFileAttributes.class);
            var perms = java.nio.file.attribute.PosixFilePermissions.toString(posix.permissions());
            var owner = posix.owner().getName();
            var group = posix.group().getName();
            var size = posix.size();
            var lastModified = posix.lastModifiedTime().toInstant();
            int linkCount = (Integer) Files.getAttribute(path, "unix:nlink");
            return new io.github.shomah4a.alle.core.input.FileAttributes(
                    perms, linkCount, owner, group, size, lastModified);
        } catch (IOException e) {
            return io.github.shomah4a.alle.core.input.FileAttributes.EMPTY;
        }
    }

    private static void loadUserInit(
            io.github.shomah4a.alle.script.ScriptEngine scriptEngine,
            io.github.shomah4a.alle.core.buffer.MessageBuffer msg,
            io.github.shomah4a.alle.core.buffer.MessageBuffer warnings) {
        Path initFile = Path.of(System.getProperty("user.home"), ".alle.d", "init.py");
        if (!Files.isRegularFile(initFile)) {
            return;
        }
        msg.message("Loading " + initFile + "...");
        try {
            String code = Files.readString(initFile, StandardCharsets.UTF_8);
            var result = scriptEngine.eval(code);
            if (result instanceof io.github.shomah4a.alle.script.ScriptResult.Failure failure) {
                msg.message("init.py error: " + failure.message());
                warnings.message("init.py error: " + failure.message());
                var sw = new java.io.StringWriter();
                failure.cause().printStackTrace(new java.io.PrintWriter(sw));
                for (String line : sw.toString().lines().toList()) {
                    warnings.message(line);
                }
            } else {
                msg.message("Loaded " + initFile);
            }
        } catch (IOException e) {
            msg.message("init.py read error: " + e.getMessage());
        }
    }

    private static void disableFlowControl() throws IOException {
        new ProcessBuilder("stty", "-ixon").inheritIO().start().onExit().join();
    }

    private static void restoreFlowControl() throws IOException {
        new ProcessBuilder("stty", "ixon").inheritIO().start().onExit().join();
    }
}
