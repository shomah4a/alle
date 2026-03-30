package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.BufferNameCompleter;
import io.github.shomah4a.alle.core.input.Completer;
import io.github.shomah4a.alle.core.input.CompletionCandidate;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * バッファを削除するコマンド。
 * ミニバッファでバッファ名を入力させ、該当バッファを削除する。
 * デフォルトは現在のバッファ。削除後は他のウィンドウで表示されていない
 * バッファに切り替える。*scratch*を削除した場合はサイレントに再作成する。
 */
public class KillBufferCommand implements Command {

    private static final Logger logger = Logger.getLogger(KillBufferCommand.class.getName());

    private static final String SCRATCH_BUFFER_NAME = "*scratch*";

    private static final Completer KILL_CONFIRM_COMPLETER = input -> Lists.immutable
            .of("yes", "no", "save and kill")
            .select(s -> s.startsWith(input))
            .collect(CompletionCandidate::terminal);

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
        var completer = new BufferNameCompleter(context.bufferManager());

        return context.inputPrompter()
                .prompt(promptMessage, "", bufferHistory, completer)
                .thenCompose(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        var input = confirmed.value();
                        var bufferName = input.isEmpty() ? currentBufferName : input;
                        return killBuffer(context, bufferName);
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

    private CompletableFuture<Void> killBuffer(CommandContext context, String bufferName) {
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
                    .prompt(prompt, "", confirmHistory, KILL_CONFIRM_COMPLETER)
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
            logger.log(Level.WARNING, message, e);
            context.handleError(message, e);
            return CompletableFuture.completedFuture(null);
        }
        doKill(context, bufferManager, bufferName, target);
        return CompletableFuture.completedFuture(null);
    }

    private void doKill(CommandContext context, BufferManager bufferManager, String bufferName, BufferFacade target) {
        bufferManager.remove(bufferName);

        // *scratch* を削除した場合はサイレントに再作成
        if (SCRATCH_BUFFER_NAME.equals(bufferName)) {
            bufferManager.add(new BufferFacade(
                    new TextBuffer(SCRATCH_BUFFER_NAME, new GapTextModel(), context.settingsRegistry())));
        }

        // 切り替え先を決定: 他のウィンドウで表示されていないバッファを優先
        var allWindows = context.frame().getWindowTree().windows();
        var replacement = findReplacementBuffer(bufferManager, allWindows, target);

        // 削除対象を表示中の全ウィンドウを切り替え
        for (var window : allWindows) {
            if (window.getBuffer().equals(target)) {
                window.setBuffer(replacement);
            }
        }

        // 全ウィンドウの previousBuffer から dangling reference をクリア
        for (var window : allWindows) {
            window.clearPreviousBufferIf(target);
        }
    }

    private BufferFacade findReplacementBuffer(
            BufferManager bufferManager, ImmutableList<Window> allWindows, BufferFacade excluded) {
        // 現在ウィンドウで表示されているバッファのセット（削除対象を除く）
        ImmutableSet<BufferFacade> displayedBuffers =
                allWindows.collect(Window::getBuffer).toSet().toImmutable().newWithout(excluded);

        // 他のウィンドウで表示されていないバッファを優先
        var candidate = bufferManager.getBuffers().detect(b -> !b.equals(excluded) && !displayedBuffers.contains(b));
        if (candidate != null) {
            return candidate;
        }

        // なければ削除対象以外の任意のバッファ
        var fallback = bufferManager.getBuffers().detect(b -> !b.equals(excluded));
        if (fallback != null) {
            return fallback;
        }

        // ここには到達しない（size <= 1 のガードがあるため）
        throw new IllegalStateException("代替バッファが見つかりません");
    }
}
