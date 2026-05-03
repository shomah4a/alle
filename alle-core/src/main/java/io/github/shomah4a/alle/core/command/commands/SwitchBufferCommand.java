package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.BufferNameCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import java.util.concurrent.CompletableFuture;

/**
 * バッファを切り替えるコマンド。
 * ミニバッファでバッファ名を入力させ、該当バッファに切り替える。
 * 直前のバッファがある場合はデフォルト値としてプロンプトに表示し、
 * 空入力時はデフォルトバッファに切り替える。
 */
public class SwitchBufferCommand implements Command {

    private final InputHistory bufferHistory;

    public SwitchBufferCommand(InputHistory bufferHistory) {
        this.bufferHistory = bufferHistory;
    }

    @Override
    public String name() {
        return "switch-to-buffer";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.frame().getActiveWindow();
        var bufferManager = context.bufferManager();
        var defaultName = resolveFirstHistoryBufferName(window, bufferManager);
        var promptMessage =
                defaultName.isEmpty() ? "Switch to buffer: " : "Switch to buffer (default " + defaultName + "): ";

        boolean ignoreCase = context.settingsRegistry().getEffective(EditorSettings.COMPLETION_IGNORE_CASE);
        var completer = new BufferNameCompleter(context.bufferManager(), ignoreCase);
        return context.inputPrompter()
                .prompt(promptMessage, "", bufferHistory, completer)
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        var input = confirmed.value();
                        var bufferName = input.isEmpty() ? defaultName : input;
                        switchBuffer(context, bufferName);
                    }
                });
    }

    private static String resolveFirstHistoryBufferName(Window window, BufferManager bufferManager) {
        for (var entry : window.getBufferHistory()) {
            var found = bufferManager.findByIdentifier(entry.identifier());
            if (found.isPresent()) {
                return found.get().getName();
            }
        }
        return "";
    }

    private void switchBuffer(CommandContext context, String bufferName) {
        if (bufferName.isEmpty()) {
            return;
        }
        var existing = context.bufferManager().findByName(bufferName);
        if (existing.isPresent()) {
            context.frame().getActiveWindow().setBuffer(existing.get());
        } else {
            var newBuffer =
                    new BufferFacade(new TextBuffer(bufferName, new GapTextModel(), context.settingsRegistry()));
            context.bufferManager().add(newBuffer);
            context.frame().getActiveWindow().setBuffer(newBuffer);
            context.messageBuffer().message("Buffer created: " + bufferName);
        }
    }
}
