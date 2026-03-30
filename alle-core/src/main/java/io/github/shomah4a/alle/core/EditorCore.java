package io.github.shomah4a.alle.core;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.command.ShutdownHandler;
import io.github.shomah4a.alle.core.command.commands.BackwardCharCommand;
import io.github.shomah4a.alle.core.command.commands.BackwardDeleteCharCommand;
import io.github.shomah4a.alle.core.command.commands.BeginningOfLineCommand;
import io.github.shomah4a.alle.core.command.commands.CommentDwimCommand;
import io.github.shomah4a.alle.core.command.commands.CommentRegionCommand;
import io.github.shomah4a.alle.core.command.commands.CopyRegionCommand;
import io.github.shomah4a.alle.core.command.commands.DedentRegionCommand;
import io.github.shomah4a.alle.core.command.commands.DeleteCharCommand;
import io.github.shomah4a.alle.core.command.commands.DeleteOtherWindowsCommand;
import io.github.shomah4a.alle.core.command.commands.DeleteWindowCommand;
import io.github.shomah4a.alle.core.command.commands.EndOfLineCommand;
import io.github.shomah4a.alle.core.command.commands.ExecuteCommandCommand;
import io.github.shomah4a.alle.core.command.commands.FindFileCommand;
import io.github.shomah4a.alle.core.command.commands.ForwardCharCommand;
import io.github.shomah4a.alle.core.command.commands.IndentDedentBackspaceCommand;
import io.github.shomah4a.alle.core.command.commands.IndentRegionCommand;
import io.github.shomah4a.alle.core.command.commands.KeyboardQuitCommand;
import io.github.shomah4a.alle.core.command.commands.KillBufferCommand;
import io.github.shomah4a.alle.core.command.commands.KillLineCommand;
import io.github.shomah4a.alle.core.command.commands.KillRegionCommand;
import io.github.shomah4a.alle.core.command.commands.NewlineCommand;
import io.github.shomah4a.alle.core.command.commands.NextLineCommand;
import io.github.shomah4a.alle.core.command.commands.OtherWindowCommand;
import io.github.shomah4a.alle.core.command.commands.PreviousLineCommand;
import io.github.shomah4a.alle.core.command.commands.ProcessQuitCommand;
import io.github.shomah4a.alle.core.command.commands.RedoCommand;
import io.github.shomah4a.alle.core.command.commands.RevertBufferCommand;
import io.github.shomah4a.alle.core.command.commands.SaveBufferCommand;
import io.github.shomah4a.alle.core.command.commands.SaveBuffersKillAlleCommand;
import io.github.shomah4a.alle.core.command.commands.ScrollDownCommand;
import io.github.shomah4a.alle.core.command.commands.ScrollUpCommand;
import io.github.shomah4a.alle.core.command.commands.SelfInsertCommand;
import io.github.shomah4a.alle.core.command.commands.SetMarkCommand;
import io.github.shomah4a.alle.core.command.commands.SplitWindowBelowCommand;
import io.github.shomah4a.alle.core.command.commands.SplitWindowRightCommand;
import io.github.shomah4a.alle.core.command.commands.SwitchBufferCommand;
import io.github.shomah4a.alle.core.command.commands.ToggleTruncateLinesCommand;
import io.github.shomah4a.alle.core.command.commands.UndoCommand;
import io.github.shomah4a.alle.core.command.commands.YankCommand;
import io.github.shomah4a.alle.core.input.DefaultFileOperations;
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
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.TextMode;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredChmodCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredChownCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredCopyCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredDeleteCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredFindFileOrToggleCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMarkCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredNoOpCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredRefreshCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredRenameCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredToggleCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredToggleMarkCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredUnmarkCommand;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredUpDirectoryCommand;
import io.github.shomah4a.alle.core.search.ISearchBackwardCommand;
import io.github.shomah4a.alle.core.search.ISearchForwardCommand;
import io.github.shomah4a.alle.core.search.ISearchHistory;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.function.Function;

/**
 * エディタのコア初期化を一括で行うファクトリ。
 * Buffer, Frame, CommandRegistry, Keymap, CommandLoop等のcore層オブジェクトを構築する。
 */
public final class EditorCore {

    private final Frame frame;
    private final BufferManager bufferManager;
    private final MessageBuffer messageBuffer;
    private final MessageBuffer warningBuffer;
    private final CommandRegistry commandRegistry;
    private final Keymap keymap;
    private final CommandLoop commandLoop;
    private final ModeRegistry modeRegistry;
    private final AutoModeMap autoModeMap;

    private EditorCore(
            Frame frame,
            BufferManager bufferManager,
            MessageBuffer messageBuffer,
            MessageBuffer warningBuffer,
            CommandRegistry commandRegistry,
            Keymap keymap,
            CommandLoop commandLoop,
            ModeRegistry modeRegistry,
            AutoModeMap autoModeMap) {
        this.frame = frame;
        this.bufferManager = bufferManager;
        this.messageBuffer = messageBuffer;
        this.warningBuffer = warningBuffer;
        this.commandRegistry = commandRegistry;
        this.keymap = keymap;
        this.commandLoop = commandLoop;
        this.modeRegistry = modeRegistry;
        this.autoModeMap = autoModeMap;
    }

    /**
     * エディタのコアを構築する。
     *
     * @param inputSource キー入力ソース
     * @param inputPrompterFactory Frameを受け取りInputPrompterを生成するファクトリ
     * @param bufferIO ファイル読み書き
     * @param directoryLister ディレクトリ一覧取得
     * @param shutdownRequestable 終了要求インターフェース
     * @param settingsRegistry 設定レジストリ
     */
    public static EditorCore create(
            InputSource inputSource,
            Function<Frame, InputPrompter> inputPrompterFactory,
            BufferIO bufferIO,
            DirectoryLister directoryLister,
            ShutdownRequestable shutdownRequestable,
            SettingsRegistry settingsRegistry) {

        // バッファ・ウィンドウ・フレーム
        var scratchFacade = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), settingsRegistry));
        var window = new Window(scratchFacade);
        var minibuffer =
                new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settingsRegistry)));
        var frame = new Frame(window, minibuffer);

        // メッセージバッファ
        var messageBuffer = new MessageBuffer("*Messages*", 1000, settingsRegistry);
        var warningBuffer = new MessageBuffer("*Warnings*", 1000, settingsRegistry);

        // バッファマネージャ
        var bufferManager = new BufferManager();
        bufferManager.add(scratchFacade);
        bufferManager.add(new BufferFacade(messageBuffer));
        bufferManager.add(new BufferFacade(warningBuffer));

        // モードマップ
        var autoModeMap = new AutoModeMap(TextMode::new);
        autoModeMap.register("md", MarkdownMode::new);
        autoModeMap.register("markdown", MarkdownMode::new);

        // モードレジストリ
        var modeRegistry = new ModeRegistry();

        // コマンドレジストリ・コマンドリゾルバ
        var shutdownHandler = new ShutdownHandler();
        var commandResolver = new CommandResolver();
        var registry = createCommandRegistry(
                bufferIO,
                directoryLister,
                autoModeMap,
                modeRegistry,
                shutdownHandler,
                shutdownRequestable,
                commandResolver);
        commandResolver.setGlobalRegistry(registry);

        // モード登録（コマンド自動登録のためCommandRegistry設定後に行う）
        modeRegistry.setCommandRegistry(registry);
        modeRegistry.registerMajorMode("text", TextMode::new);
        modeRegistry.registerMajorMode("markdown", MarkdownMode::new);

        // キーマップ
        var keymap = createKeymap(registry);
        var resolver = new KeyResolver();
        resolver.addKeymap(keymap);

        // コマンドループ
        var inputPrompter = inputPrompterFactory.apply(frame);
        var killRing = new KillRing();
        var commandLoop = new CommandLoop(
                inputSource,
                resolver,
                frame,
                bufferManager,
                inputPrompter,
                killRing,
                messageBuffer,
                warningBuffer,
                settingsRegistry,
                commandResolver);

        return new EditorCore(
                frame,
                bufferManager,
                messageBuffer,
                warningBuffer,
                registry,
                keymap,
                commandLoop,
                modeRegistry,
                autoModeMap);
    }

    private static CommandRegistry createCommandRegistry(
            BufferIO bufferIO,
            DirectoryLister directoryLister,
            AutoModeMap autoModeMap,
            ModeRegistry modeRegistry,
            ShutdownHandler shutdownHandler,
            ShutdownRequestable shutdownRequestable,
            CommandResolver commandResolver) {
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
        registry.register(new IndentDedentBackspaceCommand());
        registry.register(new IndentRegionCommand());
        registry.register(new DedentRegionCommand());
        registry.register(new CommentDwimCommand());
        registry.register(new CommentRegionCommand());
        var filePathHistory = new InputHistory();
        var findFileCommand = new FindFileCommand(
                bufferIO,
                directoryLister,
                Path.of("").toAbsolutePath(),
                autoModeMap,
                modeRegistry,
                filePathHistory,
                path -> Files.isDirectory(path));
        registry.register(findFileCommand);
        registry.register(new SaveBufferCommand(bufferIO, directoryLister, filePathHistory));
        registry.register(new RevertBufferCommand(bufferIO));
        var bufferHistory = new InputHistory();
        registry.register(new SwitchBufferCommand(bufferHistory));
        registry.register(new OtherWindowCommand());
        registry.register(new KillBufferCommand(bufferHistory, bufferIO));
        var commandHistory = new InputHistory();
        registry.register(new ExecuteCommandCommand(commandResolver, commandHistory));
        registry.register(new SetMarkCommand());
        registry.register(new KillRegionCommand());
        registry.register(new CopyRegionCommand());
        registry.register(new YankCommand());
        registry.register(new UndoCommand());
        registry.register(new RedoCommand());
        registry.register(new ScrollUpCommand());
        registry.register(new ScrollDownCommand());
        registry.register(new SplitWindowBelowCommand());
        registry.register(new SplitWindowRightCommand());
        registry.register(new DeleteWindowCommand());
        registry.register(new DeleteOtherWindowsCommand());
        registry.register(new KeyboardQuitCommand());
        registry.register(new ToggleTruncateLinesCommand());
        registry.register(new SaveBuffersKillAlleCommand(shutdownHandler, shutdownRequestable));
        registry.register(new ProcessQuitCommand(shutdownRequestable));
        var isearchHistory = new ISearchHistory();
        registry.register(new ISearchForwardCommand(isearchHistory));
        registry.register(new ISearchBackwardCommand(isearchHistory));

        // Tree Dired コマンド群（モードスコープ）
        var toggleCommand = new TreeDiredToggleCommand();
        var findFileOrToggleCommand = new TreeDiredFindFileOrToggleCommand(bufferIO, autoModeMap, modeRegistry);
        var upDirectoryCommand = new TreeDiredUpDirectoryCommand();
        var refreshCommand = new TreeDiredRefreshCommand();
        var markCommand = new TreeDiredMarkCommand();
        var unmarkCommand = new TreeDiredUnmarkCommand();
        var toggleMarkCommand = new TreeDiredToggleMarkCommand();
        var fileOperations = new DefaultFileOperations();
        var diredConfirmHistory = new InputHistory();
        var copyCommand = new TreeDiredCopyCommand(fileOperations, new InputHistory(), diredConfirmHistory);
        var renameCommand = new TreeDiredRenameCommand(fileOperations, new InputHistory());
        var deleteCommand = new TreeDiredDeleteCommand(fileOperations, diredConfirmHistory);
        var chmodCommand = new TreeDiredChmodCommand(fileOperations, new InputHistory());
        var chownCommand = new TreeDiredChownCommand(fileOperations, new InputHistory());
        var killBufferCmd = registry.lookup("kill-buffer").orElseThrow();
        var diredCommandRegistry = new CommandRegistry();
        diredCommandRegistry.register(toggleCommand);
        diredCommandRegistry.register(findFileOrToggleCommand);
        diredCommandRegistry.register(upDirectoryCommand);
        diredCommandRegistry.register(refreshCommand);
        diredCommandRegistry.register(markCommand);
        diredCommandRegistry.register(unmarkCommand);
        diredCommandRegistry.register(toggleMarkCommand);
        diredCommandRegistry.register(copyCommand);
        diredCommandRegistry.register(renameCommand);
        diredCommandRegistry.register(deleteCommand);
        diredCommandRegistry.register(chmodCommand);
        diredCommandRegistry.register(chownCommand);
        commandResolver.registerModeCommands("tree-dired", diredCommandRegistry);

        // Tree Dired キーマップ構築
        var diredKeymap = createTreeDiredKeymap(
                toggleCommand,
                findFileOrToggleCommand,
                upDirectoryCommand,
                refreshCommand,
                killBufferCmd,
                markCommand,
                unmarkCommand,
                toggleMarkCommand,
                copyCommand,
                renameCommand,
                deleteCommand,
                chmodCommand,
                chownCommand);

        var diredHistory = new InputHistory();
        var treeDiredCommand = new TreeDiredCommand(
                directoryLister,
                Path.of("").toAbsolutePath(),
                diredHistory,
                ZoneId.systemDefault(),
                diredKeymap,
                diredCommandRegistry);
        registry.register(treeDiredCommand);
        findFileCommand.setTreeDiredCommand(treeDiredCommand);
        return registry;
    }

    private static Keymap createTreeDiredKeymap(
            Command toggleCommand,
            Command findFileOrToggleCommand,
            Command upDirectoryCommand,
            Command refreshCommand,
            Command killBufferCommand,
            Command markCommand,
            Command unmarkCommand,
            Command toggleMarkCommand,
            Command copyCommand,
            Command renameCommand,
            Command deleteCommand,
            Command chmodCommand,
            Command chownCommand) {
        var keymap = new Keymap("tree-dired");
        keymap.setDefaultCommand(new TreeDiredNoOpCommand());
        keymap.bind(KeyStroke.of('\t'), toggleCommand);
        keymap.bind(KeyStroke.of('\n'), findFileOrToggleCommand);
        keymap.bind(KeyStroke.of('f'), findFileOrToggleCommand);
        keymap.bind(KeyStroke.of('^'), upDirectoryCommand);
        keymap.bind(KeyStroke.of('g'), refreshCommand);
        keymap.bind(KeyStroke.of('q'), killBufferCommand);
        keymap.bind(KeyStroke.of('m'), markCommand);
        keymap.bind(KeyStroke.of('u'), unmarkCommand);
        keymap.bind(KeyStroke.of('t'), toggleMarkCommand);
        keymap.bind(KeyStroke.of('C'), copyCommand);
        keymap.bind(KeyStroke.of('R'), renameCommand);
        keymap.bind(KeyStroke.of('D'), deleteCommand);
        keymap.bind(KeyStroke.of('M'), chmodCommand);
        keymap.bind(KeyStroke.of('O'), chownCommand);
        return keymap;
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
        keymap.bind(KeyStroke.ctrl('v'), registry.lookup("scroll-up").orElseThrow());
        keymap.bind(KeyStroke.meta('v'), registry.lookup("scroll-down").orElseThrow());
        keymap.bind(KeyStroke.ctrl('d'), registry.lookup("delete-char").orElseThrow());
        keymap.bind(KeyStroke.ctrl('k'), registry.lookup("kill-line").orElseThrow());
        keymap.bind(
                KeyStroke.of(0x7F), registry.lookup("indent-dedent-backspace").orElseThrow());
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

        // C-q (即時終了)
        keymap.bind(KeyStroke.ctrl('q'), registry.lookup("quit").orElseThrow());

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
        ctrlXMap.bind(KeyStroke.of('d'), registry.lookup("tree-dired").orElseThrow());
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

        // M-; (comment toggle)
        keymap.bind(KeyStroke.meta(';'), registry.lookup("comment-dwim").orElseThrow());

        // C-s / C-r (i-search)
        keymap.bind(KeyStroke.ctrl('s'), registry.lookup("isearch-forward").orElseThrow());
        keymap.bind(KeyStroke.ctrl('r'), registry.lookup("isearch-backward").orElseThrow());

        // C-c プレフィックスキーマップ
        var ctrlCMap = new Keymap("C-c");
        ctrlCMap.bind(KeyStroke.of('>'), registry.lookup("indent-region").orElseThrow());
        ctrlCMap.bind(KeyStroke.of('<'), registry.lookup("dedent-region").orElseThrow());
        ctrlCMap.bind(KeyStroke.of('#'), registry.lookup("comment-region").orElseThrow());
        keymap.bindPrefix(KeyStroke.ctrl('c'), ctrlCMap);

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

    public MessageBuffer warningBuffer() {
        return warningBuffer;
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

    public ModeRegistry modeRegistry() {
        return modeRegistry;
    }

    public AutoModeMap autoModeMap() {
        return autoModeMap;
    }
}
