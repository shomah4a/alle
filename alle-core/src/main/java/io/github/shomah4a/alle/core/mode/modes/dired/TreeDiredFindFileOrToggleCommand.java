package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * カーソル行がファイルなら開き、ディレクトリなら展開/折り畳みを行う。
 */
public class TreeDiredFindFileOrToggleCommand implements Command {

    private static final Logger logger = Logger.getLogger(TreeDiredFindFileOrToggleCommand.class.getName());

    private final BufferIO bufferIO;
    private final AutoModeMap autoModeMap;
    private final ModeRegistry modeRegistry;

    public TreeDiredFindFileOrToggleCommand(BufferIO bufferIO, AutoModeMap autoModeMap, ModeRegistry modeRegistry) {
        this.bufferIO = bufferIO;
        this.autoModeMap = autoModeMap;
        this.modeRegistry = modeRegistry;
    }

    @Override
    public String name() {
        return "tree-dired-find-file-or-toggle";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        var entryOpt = TreeDiredEntryResolver.resolve(context.activeWindow(), diredMode);
        if (entryOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        var entry = entryOpt.get();
        if (entry.isDirectory()) {
            diredMode.getModel().toggle(entry.path());
            TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);
            return CompletableFuture.completedFuture(null);
        }

        openFile(context, entry);
        return CompletableFuture.completedFuture(null);
    }

    private void openFile(CommandContext context, TreeDiredEntry entry) {
        var path = entry.path();

        // 同一パスのバッファが既に存在する場合はそのバッファに切り替え
        var existingBuffer = context.bufferManager().findByPath(path);
        if (existingBuffer.isPresent()) {
            context.frame().getActiveWindow().setBuffer(existingBuffer.get());
            return;
        }

        // ファイルを読み込むか、存在しなければ空バッファを作成
        BufferFacade bufferFacade;
        try {
            var loadResult = bufferIO.load(path);
            bufferFacade = loadResult.bufferFacade();
        } catch (IOException e) {
            logger.log(Level.FINE, "ファイルが存在しないため空バッファを作成: " + path, e);
            bufferFacade = new BufferFacade(new TextBuffer(
                    path.getFileName().toString(), new GapTextModel(), context.settingsRegistry(), path));
        }

        var finalBufferFacade = bufferFacade;
        var majorMode = autoModeMap.resolve(bufferFacade.getName(), () -> finalBufferFacade.lineText(0));
        bufferFacade.setMajorMode(majorMode);
        modeRegistry.runMajorModeHooks(majorMode.name(), bufferFacade);
        context.bufferManager().add(bufferFacade);
        context.frame().getActiveWindow().setBuffer(bufferFacade);
    }
}
