package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.setting.EditorSettings;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * mark〜point で決まる矩形範囲の各行を、プロンプトで入力した文字列で置き換えるコマンド。
 * Emacs の string-rectangle (C-x r t) に相当する。
 *
 * <p>空文字列入力時は矩形を削除（delete-rectangle 相当、右側テキストは詰まる）。
 *
 * <p>矩形座標はプロンプト開始前に確定してクロージャにキャプチャする。プロンプト完了後に
 * {@link io.github.shomah4a.alle.core.command.CommandLoop} が自動で clearMark を呼んでも
 * 影響を受けない。
 *
 * <p>実編集は {@link io.github.shomah4a.alle.core.buffer.BufferFacade#atomicOperation}
 * の内側で {@link io.github.shomah4a.alle.core.buffer.UndoManager#withTransaction}
 * を使い 1 undo 単位にまとめる。
 */
public class StringRectangleCommand implements Command {

    private final InputHistory history;

    public StringRectangleCommand(InputHistory history) {
        this.history = history;
    }

    @Override
    public String name() {
        return "string-rectangle";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var window = context.activeWindow();
        var buffer = window.getBuffer();
        int tabWidth = buffer.getSettings().get(EditorSettings.TAB_WIDTH);
        var rectOpt = RectangleGeometry.fromRegion(window, tabWidth);
        if (rectOpt.isEmpty()) {
            context.messageBuffer().message("No region active");
            return CompletableFuture.completedFuture(null);
        }
        // プロンプト開始前に矩形座標を確定してキャプチャ
        var rect = rectOpt.get();
        return context.inputPrompter().prompt("String rectangle: ", history).thenAccept(result -> {
            if (!(result instanceof PromptResult.Confirmed confirmed)) {
                return;
            }
            applyStringRectangle(window, window.getBuffer(), rect, confirmed.value(), tabWidth);
        });
    }

    private static void applyStringRectangle(
            io.github.shomah4a.alle.core.window.Window window,
            io.github.shomah4a.alle.core.buffer.BufferFacade buffer,
            Rectangle rect,
            String replacement,
            int tabWidth) {
        // 非同期コールバック内。CommandLoop の withTransaction は閉じているため
        // 明示的に atomicOperation + withTransaction で 1 undo 単位にまとめる。
        buffer.atomicOperation(b -> {
            b.getUndoManager().withTransaction(() -> {
                if (replacement.isEmpty()) {
                    if (rect.width() == 0) {
                        return;
                    }
                    RectangleGeometry.deleteRectangle(b, rect, tabWidth);
                } else {
                    MutableList<String> lines = Lists.mutable.empty();
                    for (int i = 0; i < rect.lineCount(); i++) {
                        lines.add(replacement);
                    }
                    RectangleGeometry.replaceRectangle(b, rect, lines, tabWidth);
                }
                window.setPoint(RectangleGeometry.topLeftOffset(b, rect, tabWidth));
                b.markDirty();
            });
            return null;
        });
    }
}
