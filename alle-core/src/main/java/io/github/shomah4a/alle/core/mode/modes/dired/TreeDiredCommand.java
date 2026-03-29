package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FilePathCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

/**
 * Tree Dired バッファを開くコマンド。
 * ミニバッファでディレクトリパスを入力させ、ツリー表示のバッファを作成する。
 */
public class TreeDiredCommand implements Command {

    private final DirectoryLister directoryLister;
    private final Path workingDirectory;
    private final InputHistory directoryHistory;
    private final ZoneId zoneId;
    private final TreeDiredToggleCommand toggleCommand;
    private final TreeDiredFindFileOrToggleCommand findFileOrToggleCommand;
    private final TreeDiredUpDirectoryCommand upDirectoryCommand;
    private final TreeDiredRefreshCommand refreshCommand;
    private final Command killBufferCommand;
    private final TreeDiredMarkCommand markCommand;
    private final TreeDiredUnmarkCommand unmarkCommand;
    private final TreeDiredToggleMarkCommand toggleMarkCommand;

    public TreeDiredCommand(
            DirectoryLister directoryLister,
            Path workingDirectory,
            InputHistory directoryHistory,
            ZoneId zoneId,
            TreeDiredToggleCommand toggleCommand,
            TreeDiredFindFileOrToggleCommand findFileOrToggleCommand,
            TreeDiredUpDirectoryCommand upDirectoryCommand,
            TreeDiredRefreshCommand refreshCommand,
            Command killBufferCommand,
            TreeDiredMarkCommand markCommand,
            TreeDiredUnmarkCommand unmarkCommand,
            TreeDiredToggleMarkCommand toggleMarkCommand) {
        this.directoryLister = directoryLister;
        this.workingDirectory = workingDirectory;
        this.directoryHistory = directoryHistory;
        this.zoneId = zoneId;
        this.toggleCommand = toggleCommand;
        this.findFileOrToggleCommand = findFileOrToggleCommand;
        this.upDirectoryCommand = upDirectoryCommand;
        this.refreshCommand = refreshCommand;
        this.killBufferCommand = killBufferCommand;
        this.markCommand = markCommand;
        this.unmarkCommand = unmarkCommand;
        this.toggleMarkCommand = toggleMarkCommand;
    }

    @Override
    public String name() {
        return "tree-dired";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var completer = new FilePathCompleter(directoryLister);
        String initialValue = workingDirectory + "/";
        return context.inputPrompter()
                .prompt("Dired (directory): ", initialValue, directoryHistory, completer)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        openDired(context, confirmed.value());
                    }
                });
    }

    /**
     * 指定パスのディレクトリを Tree Dired バッファで開く。
     * find-file からの委譲用。
     */
    public void openDiredForPath(CommandContext context, Path directory) {
        openDired(context, directory.toAbsolutePath().normalize().toString());
    }

    private void openDired(CommandContext context, String pathString) {
        if (pathString.isEmpty()) {
            return;
        }
        var path = Path.of(pathString).toAbsolutePath().normalize();

        // 末尾の "/" を除去してディレクトリパスとして扱う
        String bufferName = "*Dired " + path + "*";

        // 同名バッファが既にある場合はそこに切り替え
        var existing = context.bufferManager().findByName(bufferName);
        if (existing.isPresent()) {
            context.frame().getActiveWindow().setBuffer(existing.get());
            return;
        }

        // バッファ作成
        var textBuffer = new TextBuffer(bufferName, new GapTextModel(), context.settingsRegistry());
        var bufferFacade = new BufferFacade(textBuffer);

        // モデル・モード作成
        var model = new TreeDiredModel(path, directoryLister);
        var keymap = createKeymap();
        var mode = new TreeDiredMode(model, keymap, zoneId);
        bufferFacade.setMajorMode(mode);

        // バッファを登録してウィンドウに表示
        context.bufferManager().add(bufferFacade);
        context.frame().getActiveWindow().setBuffer(bufferFacade);

        // 初期レンダリング（read-onlyは設定前に行う）
        TreeDiredRenderer.render(bufferFacade, model.getRootDirectory(), model.getVisibleEntries(), zoneId);
        bufferFacade.setReadOnly(true);
        bufferFacade.markClean();
        context.activeWindow().setPoint(0);
    }

    private Keymap createKeymap() {
        var keymap = new Keymap("tree-dired");

        // no-op defaultCommand で self-insert を抑制
        keymap.setDefaultCommand(new NoOpCommand());

        // TAB: toggle
        keymap.bind(KeyStroke.of('\t'), toggleCommand);
        // Enter: find-file-or-toggle
        keymap.bind(KeyStroke.of('\n'), findFileOrToggleCommand);
        // f: find-file-or-toggle
        keymap.bind(KeyStroke.of('f'), findFileOrToggleCommand);
        // ^: up-directory
        keymap.bind(KeyStroke.of('^'), upDirectoryCommand);
        // g: refresh
        keymap.bind(KeyStroke.of('g'), refreshCommand);
        // q: kill-buffer
        keymap.bind(KeyStroke.of('q'), killBufferCommand);
        // m: mark
        keymap.bind(KeyStroke.of('m'), markCommand);
        // u: unmark
        keymap.bind(KeyStroke.of('u'), unmarkCommand);
        // t: toggle-mark
        keymap.bind(KeyStroke.of('t'), toggleMarkCommand);

        return keymap;
    }

    /**
     * 何もしないコマンド。未バインドキーの self-insert 抑制用。
     */
    private static class NoOpCommand implements Command {

        @Override
        public String name() {
            return "tree-dired-noop";
        }

        @Override
        public CompletableFuture<Void> execute(CommandContext context) {
            return CompletableFuture.completedFuture(null);
        }
    }
}
