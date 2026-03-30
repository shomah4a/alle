package io.github.shomah4a.alle.core.mode.modes.occur;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Direction;
import java.util.concurrent.CompletableFuture;

/**
 * occurコマンド。
 * ミニバッファでクエリを入力し、マッチ行一覧をoccurバッファに表示する。
 */
public class OccurCommand implements Command {

    private final InputHistory occurHistory;
    private final Keymap occurKeymap;
    private final CommandRegistry occurCommandRegistry;
    private final SettingsRegistry settingsRegistry;

    public OccurCommand(
            InputHistory occurHistory,
            Keymap occurKeymap,
            CommandRegistry occurCommandRegistry,
            SettingsRegistry settingsRegistry) {
        this.occurHistory = occurHistory;
        this.occurKeymap = occurKeymap;
        this.occurCommandRegistry = occurCommandRegistry;
        this.settingsRegistry = settingsRegistry;
    }

    @Override
    public String name() {
        return "occur";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        return context.inputPrompter().prompt("Occur: ", occurHistory).thenAccept(result -> {
            if (result instanceof PromptResult.Confirmed confirmed) {
                runOccur(context, confirmed.value());
            }
        });
    }

    private void runOccur(CommandContext context, String query) {
        if (query.isEmpty()) {
            return;
        }

        BufferFacade sourceBuffer = context.activeWindow().getBuffer();
        String sourceBufferName = sourceBuffer.getName();
        String occurBufferName = "*Occur " + sourceBufferName + "*";

        // 検索実行
        OccurModel model = OccurModel.search(sourceBuffer, query);

        // 同名バッファが既存ならread-only解除して上書き、なければ新規作成
        var existingOpt = context.bufferManager().findByName(occurBufferName);
        BufferFacade occurBuffer;
        boolean isNew;

        if (existingOpt.isPresent()) {
            occurBuffer = existingOpt.get();
            isNew = false;
        } else {
            var textBuffer = new TextBuffer(occurBufferName, new GapTextModel(), settingsRegistry);
            occurBuffer = new BufferFacade(textBuffer);
            isNew = true;
        }

        // モード設定
        var mode = new OccurMode(model, occurKeymap, occurCommandRegistry);
        occurBuffer.setMajorMode(mode);

        // レンダリング（read-only一時解除）
        occurBuffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            OccurRenderer.render(buf, model);
            buf.markClean();
            buf.setReadOnly(true);
            return null;
        });

        if (isNew) {
            context.bufferManager().add(occurBuffer);
            // ウィンドウを上下分割して下側にoccurバッファを表示
            context.frame().splitActiveWindow(Direction.HORIZONTAL, occurBuffer);
        } else {
            // 既存バッファの場合、既にoccurウィンドウがあればそちらにフォーカス
            var windows = context.frame().getWindowTree().windows();
            boolean found = false;
            for (var window : windows) {
                if (window.getBuffer().equals(occurBuffer)) {
                    context.frame().setActiveWindow(window);
                    window.setPoint(0);
                    found = true;
                    break;
                }
            }
            if (!found) {
                context.frame().splitActiveWindow(Direction.HORIZONTAL, occurBuffer);
            }
        }

        context.activeWindow().setPoint(0);
    }
}
