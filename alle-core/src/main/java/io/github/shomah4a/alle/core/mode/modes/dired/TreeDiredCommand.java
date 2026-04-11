package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
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
    private final Keymap diredKeymap;
    private final CommandRegistry diredCommandRegistry;
    private final FilePathInputPrompter filePathInputPrompter;

    public TreeDiredCommand(
            DirectoryLister directoryLister,
            Path workingDirectory,
            InputHistory directoryHistory,
            ZoneId zoneId,
            Keymap diredKeymap,
            CommandRegistry diredCommandRegistry,
            FilePathInputPrompter filePathInputPrompter) {
        this.directoryLister = directoryLister;
        this.workingDirectory = workingDirectory;
        this.directoryHistory = directoryHistory;
        this.zoneId = zoneId;
        this.diredKeymap = diredKeymap;
        this.diredCommandRegistry = diredCommandRegistry;
        this.filePathInputPrompter = filePathInputPrompter;
    }

    @Override
    public String name() {
        return "tree-dired";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var defaultDir = context.activeWindow().getBuffer().getDefaultDirectory(workingDirectory);
        return filePathInputPrompter
                .prompt("Dired (directory): ", defaultDir.toString(), directoryHistory)
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

        // バッファ作成（ディレクトリパスをfilePathに設定し、find-file等の起点にする）
        var textBuffer = new TextBuffer(bufferName, new GapTextModel(), context.settingsRegistry(), path);
        var bufferFacade = new BufferFacade(textBuffer);

        // モデル・モード作成
        var model = new TreeDiredModel(path, directoryLister);
        var mode = new TreeDiredMode(model, diredKeymap, zoneId, diredCommandRegistry);
        bufferFacade.setMajorMode(mode);

        // バッファを登録してウィンドウに表示
        context.bufferManager().add(bufferFacade);
        context.frame().getActiveWindow().setBuffer(bufferFacade);

        // 初期レンダリング（read-onlyは設定前に行う）
        var customColumns = TreeDiredBufferUpdater.resolveCustomColumns(bufferFacade);
        var rootSuffix = TreeDiredBufferUpdater.resolveRootSuffix(bufferFacade);
        TreeDiredRenderer.render(
                bufferFacade, model.getRootDirectory(), model.getVisibleEntries(), zoneId, customColumns, rootSuffix);
        bufferFacade.setReadOnly(true);
        bufferFacade.markClean();
        context.activeWindow().setPoint(0);
    }
}
