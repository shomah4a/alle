package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.input.BufferNameCompleter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.util.concurrent.CompletableFuture;
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
        return context.activeWindowActor().getBufferName().thenCompose(currentBufferName -> {
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

        // 削除実行
        bufferManager.remove(bufferName);

        // *scratch* を削除した場合はサイレントに再作成
        if (SCRATCH_BUFFER_NAME.equals(bufferName)) {
            bufferManager.add(new EditableBuffer(SCRATCH_BUFFER_NAME, new GapTextModel()));
        }

        // 切り替え先を決定 → 全ウィンドウ差し替え → dangling reference クリア
        var frameActor = context.frameActor();
        return frameActor.getDisplayedBufferNames().thenCompose(displayedNames -> {
            var replacement = findReplacementBuffer(bufferManager, displayedNames, target);
            return frameActor
                    .replaceBufferInAllWindows(target, replacement)
                    .thenCompose(v -> frameActor.clearPreviousBufferInAllWindows(target));
        });
    }

    private Buffer findReplacementBuffer(
            BufferManager bufferManager, ImmutableSet<String> displayedNames, Buffer excluded) {
        // 他のウィンドウで表示されていないバッファを優先
        var candidate =
                bufferManager.getBuffers().detect(b -> !b.equals(excluded) && !displayedNames.contains(b.getName()));
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
