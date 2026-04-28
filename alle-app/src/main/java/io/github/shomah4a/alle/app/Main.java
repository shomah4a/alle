package io.github.shomah4a.alle.app;

import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ansi.UnixLikeTerminal;
import com.googlecode.lanterna.terminal.ansi.UnixTerminal;
import io.github.shomah4a.alle.core.EditorCore;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.input.DirectoryEntry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FileAttributes;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.MinibufferInputPrompter;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.server.ServerEditCommand;
import io.github.shomah4a.alle.core.server.ServerManager;
import io.github.shomah4a.alle.core.server.ServerMinorMode;
import io.github.shomah4a.alle.core.server.ServerStartCommand;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.styling.DefaultFaceTheme;
import io.github.shomah4a.alle.script.EditorFacade;
import io.github.shomah4a.alle.script.EvalExpressionCommand;
import io.github.shomah4a.alle.script.MessageBufferOutputStream;
import io.github.shomah4a.alle.script.ScriptEngine;
import io.github.shomah4a.alle.script.ScriptResult;
import io.github.shomah4a.alle.script.graalpy.GraalPyEngineFactory;
import io.github.shomah4a.alle.tui.EditorRunner;
import io.github.shomah4a.alle.tui.FaceResolver;
import io.github.shomah4a.alle.tui.RedrawDisplayCommand;
import io.github.shomah4a.alle.tui.ScreenRenderer;
import io.github.shomah4a.alle.tui.TerminalInputSource;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;

/**
 * Alleエディタのエントリポイント。
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) throws IOException {
        var options = new Options();
        options.addOption("c", "client", false, "クライアントモードで起動する");

        CommandLine cmd;
        try {
            cmd = new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            System.err.println("alle: " + e.getMessage());
            System.exit(1);
            return;
        }

        if (cmd.hasOption("client")) {
            var remaining = cmd.getArgs();
            if (remaining.length < 1) {
                System.err.println("Usage: alle --client <file>");
                System.exit(1);
            }
            ClientMain.run(remaining[0]);
            return;
        }

        // C-s/C-q がフロー制御（XON/XOFF）に奪われるのを防ぐ
        disableFlowControl();
        // C-z が SIGTSTP でプロセスをサスペンドするのを防ぐ
        disableSuspend();

        var terminal =
                new UnixTerminal(System.in, System.out, StandardCharsets.UTF_8, UnixLikeTerminal.CtrlCBehaviour.TRAP);
        // ESC単独入力後に次のキーを待つ猶予（4 × 250ms = 1秒）
        // ESC + x で M-x として解釈するために必要
        terminal.getInputDecoder().setTimeoutUnits(4);
        Screen screen = new TerminalScreen(terminal);
        try {
            screen.startScreen();
            run(screen, cmd.getArgs());
        } finally {
            screen.stopScreen();
            restoreFlowControl();
            restoreSuspend();
        }
    }

    private static void run(Screen screen, String[] args) throws IOException {
        var inputSource = new TerminalInputSource(screen);
        var settingsRegistry = new SettingsRegistry();
        settingsRegistry.register(EditorSettings.INDENT_WIDTH);
        settingsRegistry.register(EditorSettings.COMMENT_STRING);
        settingsRegistry.register(EditorSettings.TAB_WIDTH);
        var bufferIO = new BufferIO(
                source -> new BufferedReader(Files.newBufferedReader(Path.of(source), StandardCharsets.UTF_8)),
                destination ->
                        new BufferedWriter(Files.newBufferedWriter(Path.of(destination), StandardCharsets.UTF_8)),
                settingsRegistry);
        DirectoryLister directoryLister = Main::listDirectory;

        var homeDirectory = Path.of(System.getProperty("user.home"));
        var core = EditorCore.create(
                inputSource,
                MinibufferInputPrompter::new,
                bufferIO,
                directoryLister,
                inputSource,
                settingsRegistry,
                homeDirectory);

        // スクリプトエンジンの初期化
        var msg = core.messageBuffer();
        msg.message("Initializing script engine...");
        var editorFacade = new EditorFacade(
                core.frame(),
                msg,
                core.commandRegistry(),
                core.keymap(),
                core.modeRegistry(),
                core.autoModeMap(),
                core.syntaxAnalyzerRegistry(),
                core.frameLayoutStore(),
                core.bufferManager(),
                core.inputPrompter());
        var stdoutStream =
                new MessageBufferOutputStream(core.bufferManager(), "*Python Output*", 1000, settingsRegistry);
        var stderrStream =
                new MessageBufferOutputStream(core.bufferManager(), "*Python Error*", 1000, settingsRegistry);
        var logStream = new MessageBufferOutputStream(core.bufferManager(), "*Python Log*", 1000, settingsRegistry);
        msg.message("Creating GraalPy engine...");
        var alleDotD = homeDirectory.resolve(".alle.d");
        var scriptEngineFactory =
                new GraalPyEngineFactory(editorFacade, alleDotD, stdoutStream, stderrStream, logStream);
        var scriptEngine = scriptEngineFactory.create();
        msg.message("Script engine initialized.");

        // ユーザー初期化スクリプトの読み込み
        loadUserInit(scriptEngine, alleDotD, msg, core.warningBuffer());

        // eval-expression コマンド (M-:)
        var evalHistory = new InputHistory();
        core.commandRegistry().register(new EvalExpressionCommand(scriptEngine, evalHistory));
        core.keymap()
                .bind(
                        KeyStroke.meta(':'),
                        core.commandRegistry().lookup("eval-expression").orElseThrow());

        // コマンドライン引数で指定されたファイルを開く
        if (args.length > 0) {
            core.pathOpenService().open(args[0], core.bufferManager(), core.frame());
        }

        var actionQueue = new java.util.concurrent.LinkedBlockingQueue<Runnable>();

        // server-start コマンドの登録
        var serverManager = new ServerManager();
        var serverEditCommand = new ServerEditCommand(bufferIO, serverManager);
        var serverStartCommand = new ServerStartCommand(() -> serverManager.start(
                actionQueue,
                core.pathOpenService(),
                core.bufferManager(),
                core.frame(),
                () -> new ServerMinorMode(serverEditCommand)));
        core.commandRegistry().register(serverStartCommand);

        // redraw-display コマンド (C-l)
        var fullRedrawRequested = new AtomicBoolean(false);
        var redrawCommand = new RedrawDisplayCommand(fullRedrawRequested);
        core.commandRegistry().register(redrawCommand);
        core.keymap()
                .bind(
                        KeyStroke.ctrl('l'),
                        core.commandRegistry().lookup("redraw-display").orElseThrow());

        var renderer = new ScreenRenderer(screen, new DefaultFaceTheme(), new FaceResolver());
        var runner = new EditorRunner(
                inputSource,
                screen,
                renderer,
                core.commandLoop(),
                core.frame(),
                core.messageBuffer(),
                core.statusLineRenderer(),
                actionQueue,
                fullRedrawRequested);

        try {
            runner.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            serverManager.close();
            scriptEngine.close();
            scriptEngineFactory.close();
        }
    }

    private static ListIterable<DirectoryEntry> listDirectory(Path directory) throws IOException {
        var entries = Lists.mutable.<DirectoryEntry>empty();
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

    private static FileAttributes readFileAttributes(Path path) {
        try {
            var posix = Files.readAttributes(path, PosixFileAttributes.class);
            var perms = PosixFilePermissions.toString(posix.permissions());
            var owner = posix.owner().getName();
            var group = posix.group().getName();
            var size = posix.size();
            var lastModified = posix.lastModifiedTime().toInstant();
            int linkCount = (Integer) Files.getAttribute(path, "unix:nlink");
            return new FileAttributes(perms, linkCount, owner, group, size, lastModified);
        } catch (IOException e) {
            return FileAttributes.EMPTY;
        }
    }

    private static void loadUserInit(
            ScriptEngine scriptEngine, Path alleDotD, MessageBuffer msg, MessageBuffer warnings) {
        msg.message("Loading user init...");
        var result = scriptEngine.loadUserInit(alleDotD);
        if (result instanceof ScriptResult.Failure failure) {
            msg.message("init.py error: " + failure.message());
            warnings.message("init.py error: " + failure.message());
            var sw = new StringWriter();
            failure.cause().printStackTrace(new PrintWriter(sw));
            for (String line : sw.toString().lines().toList()) {
                warnings.message(line);
            }
        } else {
            msg.message("User init loaded.");
        }
    }

    private static void disableFlowControl() throws IOException {
        new ProcessBuilder("stty", "-ixon").inheritIO().start().onExit().join();
    }

    private static void restoreFlowControl() throws IOException {
        new ProcessBuilder("stty", "ixon").inheritIO().start().onExit().join();
    }

    private static void disableSuspend() throws IOException {
        new ProcessBuilder("stty", "susp", "undef").inheritIO().start().onExit().join();
    }

    private static void restoreSuspend() throws IOException {
        new ProcessBuilder("stty", "susp", "^Z").inheritIO().start().onExit().join();
    }
}
