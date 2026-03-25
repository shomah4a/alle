package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.input.BufferNameCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * バッファを削除するコマンド。
 * ミニバッファでバッファ名を入力させ、該当バッファを削除する。
 * デフォルトは現在のバッファ。削除後は他のウィンドウで表示されていない
 * バッファに切り替える。*scratch*を削除した場合はサイレントに再作成する。
 */
public class KillBufferCommand implements Command {

    private static final String SCRATCH_BUFFER_NAME = "*scratch*";

    private final InputHistory bufferHistory;

    public KillBufferCommand(InputHistory bufferHistory) {
        this.bufferHistory = bufferHistory;
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
                .thenAccept(result -> {
                    if (result instanceof PromptResult.Confirmed confirmed) {
                        var input = confirmed.value();
                        var bufferName = input.isEmpty() ? currentBufferName : input;
                        killBuffer(context, bufferName);
                    }
                });
    }

    private void killBuffer(CommandContext context, String bufferName) {
        var bufferManager = context.bufferManager();
        var targetOpt = bufferManager.findByName(bufferName);
        if (targetOpt.isEmpty()) {
            return;
        }
        var target = targetOpt.get();

        if (target.isSystemBuffer()) {
            context.messageBuffer().message("Cannot kill system buffer: " + bufferName);
            return;
        }

        if (bufferManager.size() <= 1) {
            return;
        }

        // 削除実行
        bufferManager.remove(bufferName);

        // *scratch* を削除した場合はサイレントに再作成
        if (SCRATCH_BUFFER_NAME.equals(bufferName)) {
            bufferManager.add(new BufferFacade(
                    new EditableBuffer(SCRATCH_BUFFER_NAME, new GapTextModel(), context.settingsRegistry())));
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
