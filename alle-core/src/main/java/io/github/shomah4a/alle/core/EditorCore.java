package io.github.shomah4a.alle.core;

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
import io.github.shomah4a.alle.core.command.KillRing;
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
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.input.ShutdownRequestable;
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
import java.nio.file.Path;
import java.util.function.Function;

/**
 * エディタのコア初期化を一括で行うファクトリ。
 * Buffer, Frame, CommandRegistry, Keymap, CommandLoop等のcore層オブジェクトを構築する。
 * TUI固有のコマンド・キーバインドはapp層で追加する。
 */
public final class EditorCore {

    private final Frame frame;
    private final BufferManager bufferManager;
    private final MessageBuffer messageBuffer;
    private final CommandRegistry commandRegistry;
    private final Keymap keymap;
    private final CommandLoop commandLoop;

    private EditorCore(
            Frame frame,
            BufferManager bufferManager,
            MessageBuffer messageBuffer,
            CommandRegistry commandRegistry,
            Keymap keymap,
            CommandLoop commandLoop) {
        this.frame = frame;
        this.bufferManager = bufferManager;
        this.messageBuffer = messageBuffer;
        this.commandRegistry = commandRegistry;
        this.keymap = keymap;
        this.commandLoop = commandLoop;
    }

    /**
     * エディタのコアを構築する。
     *
     * @param inputSource キー入力ソース
     * @param inputPrompterFactory Frameを受け取りInputPrompterを生成するファクトリ
     * @param bufferIO ファイル読み書き
     * @param directoryLister ディレクトリ一覧取得
     * @param shutdownRequestable 終了要求インターフェース
     */
    public static EditorCore create(
            InputSource inputSource,
            Function<Frame, InputPrompter> inputPrompterFactory,
            BufferIO bufferIO,
            DirectoryLister directoryLister,
            ShutdownRequestable shutdownRequestable) {
        // バッファ・ウィンドウ・フレーム
        var buffer = new EditableBuffer("*scratch*", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        var frame = new Frame(window, minibuffer);

        // メッセージバッファ
        var messageBuffer = new MessageBuffer("*Messages*", 1000);
        var warningBuffer = new MessageBuffer("*Warnings*", 1000);

        // バッファマネージャ
        var bufferManager = new BufferManager();
        bufferManager.add(buffer);
        bufferManager.add(messageBuffer);
        bufferManager.add(warningBuffer);

        // モードマップ
        var autoModeMap = new AutoModeMap(TextMode::new);
        autoModeMap.register("md", MarkdownMode::new);
        autoModeMap.register("markdown", MarkdownMode::new);

        // コマンドレジストリ
        var shutdownHandler = new ShutdownHandler();
        var registry =
                createCommandRegistry(bufferIO, directoryLister, autoModeMap, shutdownHandler, shutdownRequestable);

        // キーマップ
        var keymap = createKeymap(registry);
        var resolver = new KeyResolver();
        resolver.addKeymap(keymap);

        // コマンドループ
        var inputPrompter = inputPrompterFactory.apply(frame);
        var killRing = new KillRing();
        var commandLoop = new CommandLoop(
                inputSource, resolver, frame, bufferManager, inputPrompter, killRing, messageBuffer, warningBuffer);

        return new EditorCore(frame, bufferManager, messageBuffer, registry, keymap, commandLoop);
    }

    private static CommandRegistry createCommandRegistry(
            BufferIO bufferIO,
            DirectoryLister directoryLister,
            AutoModeMap autoModeMap,
            ShutdownHandler shutdownHandler,
            ShutdownRequestable shutdownRequestable) {
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
        var filePathHistory = new InputHistory();
        registry.register(new FindFileCommand(
                bufferIO, directoryLister, Path.of("").toAbsolutePath(), autoModeMap, filePathHistory));
        registry.register(new SaveBufferCommand(bufferIO, directoryLister, filePathHistory));
        var bufferHistory = new InputHistory();
        registry.register(new SwitchBufferCommand(bufferHistory));
        registry.register(new OtherWindowCommand());
        registry.register(new KillBufferCommand(bufferHistory));
        var commandHistory = new InputHistory();
        registry.register(new ExecuteCommandCommand(registry, commandHistory));
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
        registry.register(new SaveBuffersKillAlleCommand(shutdownHandler, shutdownRequestable));
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

    public Frame frame() {
        return frame;
    }

    public BufferManager bufferManager() {
        return bufferManager;
    }

    public MessageBuffer messageBuffer() {
        return messageBuffer;
    }

    public CommandRegistry commandRegistry() {
        return commandRegistry;
    }

    public Keymap keymap() {
        return keymap;
    }

    public CommandLoop commandLoop() {
        return commandLoop;
    }
}
