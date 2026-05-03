package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferKiller;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.BufferNameCompleter;
import io.github.shomah4a.alle.core.input.Completer;
import io.github.shomah4a.alle.core.input.CompletionCandidate;
import io.github.shomah4a.alle.core.input.CompletionMatching;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;

/**
 * バッファを削除するコマンド。
 * ミニバッファでバッファ名を入力させ、該当バッファを削除する。
 * デフォルトは現在のバッファ。削除後は他のウィンドウで表示されていない
 * バッファに切り替える。*scratch*を削除した場合はサイレントに再作成する。
 */
public class KillBufferCommand implements Command, Loggable {

    private static Completer createKillConfirmCompleter(boolean ignoreCase) {
        return input -> Lists.immutable
                .of("yes", "no", "save and kill")
                .select(s -> CompletionMatching.startsWith(s, input, ignoreCase))
                .collect(CompletionCandidate::terminal);
    }

    private final InputHistory bufferHistory;
    private final BufferIO bufferIO;
    private final InputHistory confirmHistory;

    public KillBufferCommand(InputHistory bufferHistory, BufferIO bufferIO) {
        this.bufferHistory = bufferHistory;
        this.bufferIO = bufferIO;
        this.confirmHistory = new InputHistory();
    }

    @Override
    public String name() {
        return "kill-buffer";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var currentBufferName = context.frame().getActiveWindow().getBuffer().getName();
        var promptMessage = "Kill buffer (default " + currentBufferName + "): ";
        boolean ignoreCase = context.settingsRegistry().getEffective(EditorSettings.COMPLETION_IGNORE_CASE);
        var completer = new BufferNameCompleter(context.bufferManager(), ignoreCase);

        return context.inputPrompter()
                .prompt(promptMessage, "", bufferHistory, completer)
                .thenCompose(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        var input = confirmed.value();
                        var bufferName = input.isEmpty() ? currentBufferName : input;
                        return killBuffer(context, bufferName, ignoreCase);
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    private CompletableFuture<Void> killBuffer(CommandContext context, String bufferName, boolean ignoreCase) {
        var bufferManager = context.bufferManager();
        var targetOpt = bufferManager.findByName(bufferName);
        if (targetOpt.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        var target = targetOpt.get();

        if (target.isSystemBuffer()) {
            context.messageBuffer().message("Cannot kill system buffer: " + bufferName);
            return CompletableFuture.completedFuture(null);
        }

        if (bufferManager.size() <= 1) {
            return CompletableFuture.completedFuture(null);
        }

        if (target.isDirty()) {
            var prompt = "Buffer " + bufferName + " modified; kill anyway? (yes, no, save and kill) ";
            return context.inputPrompter()
                    .prompt(prompt, "", confirmHistory, createKillConfirmCompleter(ignoreCase))
                    .thenCompose(confirmResult -> {
                        if (confirmResult instanceof PromptResult.Confirmed confirmed) {
                            return switch (confirmed.value()) {
                                case "yes" -> {
                                    doKill(context, bufferManager, bufferName, target);
                                    yield CompletableFuture.completedFuture(null);
                                }
                                case "save and kill" -> {
                                    yield saveAndKill(context, bufferManager, bufferName, target);
                                }
                                default -> CompletableFuture.completedFuture(null);
                            };
                        }
                        return CompletableFuture.completedFuture(null);
                    });
        }

        doKill(context, bufferManager, bufferName, target);
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<Void> saveAndKill(
            CommandContext context, BufferManager bufferManager, String bufferName, BufferFacade target) {
        if (target.getFilePath().isEmpty()) {
            context.messageBuffer().message("Buffer has no file path; use save-buffer first: " + bufferName);
            return CompletableFuture.completedFuture(null);
        }
        try {
            bufferIO.save(target);
        } catch (IOException e) {
            var message = "バッファの保存に失敗: " + bufferName;
            logger().warn(message, e);
            context.handleError(message, e);
            return CompletableFuture.completedFuture(null);
        }
        doKill(context, bufferManager, bufferName, target);
        return CompletableFuture.completedFuture(null);
    }

    private void doKill(CommandContext context, BufferManager bufferManager, String bufferName, BufferFacade target) {
        BufferKiller.kill(bufferManager, context.frame(), bufferName, target, context.settingsRegistry());
    }
}
