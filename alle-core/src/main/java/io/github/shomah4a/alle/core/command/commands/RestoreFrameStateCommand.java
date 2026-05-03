package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FrameLayoutNameCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.window.FrameLayoutStore;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * 保存済みフレーム状態を名前で復元するコマンド。
 * ミニバッファで名前を入力（補完あり）し、FrameLayoutStoreからスナップショットを取得して復元する。
 */
public class RestoreFrameStateCommand implements Command {

    private final FrameLayoutStore layoutStore;
    private final InputHistory inputHistory;
    private final Supplier<BufferFacade> fallbackBufferSupplier;

    /**
     * @param layoutStore レイアウト保存ストア
     * @param inputHistory 入力履歴
     * @param fallbackBufferSupplier バッファ不在時のフォールバックバッファ供給元
     */
    public RestoreFrameStateCommand(
            FrameLayoutStore layoutStore, InputHistory inputHistory, Supplier<BufferFacade> fallbackBufferSupplier) {
        this.layoutStore = layoutStore;
        this.inputHistory = inputHistory;
        this.fallbackBufferSupplier = fallbackBufferSupplier;
    }

    @Override
    public String name() {
        return "restore-frame-state";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        boolean ignoreCase = context.settingsRegistry().getEffective(EditorSettings.COMPLETION_IGNORE_CASE);
        var completer = new FrameLayoutNameCompleter(layoutStore, ignoreCase);
        return context.inputPrompter()
                .prompt("Restore frame state: ", "", inputHistory, completer)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        var layoutName = confirmed.value();
                        if (layoutName.isEmpty()) {
                            context.messageBuffer().message("名前が空です");
                            return;
                        }
                        var snapshot = layoutStore.load(layoutName);
                        if (snapshot.isEmpty()) {
                            context.messageBuffer().message("Frame state not found: " + layoutName);
                            return;
                        }
                        var fallback = fallbackBufferSupplier.get();
                        context.frame().restoreSnapshot(snapshot.get(), context.bufferManager(), fallback);
                        context.messageBuffer().message("Frame state restored: " + layoutName);
                    }
                });
    }
}
