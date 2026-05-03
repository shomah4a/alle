package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.FrameLayoutNameCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.window.FrameLayoutStore;
import java.util.concurrent.CompletableFuture;

/**
 * 現在のフレーム状態を名前付きで保存するコマンド。
 * ミニバッファで名前を入力し、フレームのスナップショットをFrameLayoutStoreに保存する。
 */
public class SaveFrameStateCommand implements Command {

    private final FrameLayoutStore layoutStore;
    private final InputHistory inputHistory;

    public SaveFrameStateCommand(FrameLayoutStore layoutStore, InputHistory inputHistory) {
        this.layoutStore = layoutStore;
        this.inputHistory = inputHistory;
    }

    @Override
    public String name() {
        return "save-frame-state";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        boolean ignoreCase = context.settingsRegistry().getEffective(EditorSettings.COMPLETION_IGNORE_CASE);
        var completer = new FrameLayoutNameCompleter(layoutStore, ignoreCase);
        return context.inputPrompter()
                .prompt("Save frame state as: ", "", inputHistory, completer)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        var layoutName = confirmed.value();
                        if (layoutName.isEmpty()) {
                            context.messageBuffer().message("名前が空です");
                            return;
                        }
                        var snapshot = context.frame().captureSnapshot();
                        layoutStore.save(layoutName, snapshot);
                        context.messageBuffer().message("Frame state saved: " + layoutName);
                    }
                });
    }
}
