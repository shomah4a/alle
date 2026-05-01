package io.github.shomah4a.alle.core.command.commands;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.input.ShellCommandExecutor;
import io.github.shomah4a.alle.core.input.ShellOutputBufferHelper;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Direction;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * シェルコマンドを実行し、結果を {@code *Shell Command Output*} バッファに表示するコマンド。
 * Emacs の {@code shell-command} (M-!) に相当する。
 *
 * <p>実行は非同期で行われ、stdout/stderr はそれぞれ別スレッドでブロッキングリードされる。
 * stderr は WARNING face で色分けされる。
 * 出力バッファは画面を下に分割して表示される。
 */
public class ShellCommandCommand implements Command, Loggable {

    private static final String OUTPUT_BUFFER_NAME = "*Shell Command Output*";

    private final ShellCommandExecutor executor;
    private final InputHistory shellHistory;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ShellCommandCommand(ShellCommandExecutor executor, InputHistory shellHistory) {
        this.executor = executor;
        this.shellHistory = shellHistory;
    }

    @Override
    public String name() {
        return "shell-command";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        if (running.get()) {
            context.messageBuffer().message("シェルコマンドを実行中です");
            return CompletableFuture.completedFuture(null);
        }

        return context.inputPrompter()
                .prompt("Shell command: ", shellHistory)
                .thenCompose(result -> handlePromptResult(context, result));
    }

    private CompletableFuture<Void> handlePromptResult(CommandContext context, PromptResult result) {
        if (!(result instanceof PromptResult.Confirmed confirmed)) {
            return CompletableFuture.completedFuture(null);
        }
        String command = confirmed.value().trim();
        if (command.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        if (!running.compareAndSet(false, true)) {
            context.messageBuffer().message("シェルコマンドを実行中です");
            return CompletableFuture.completedFuture(null);
        }

        var outputBuffer = getOrCreateOutputBuffer(context);
        ShellOutputBufferHelper.clearBuffer(outputBuffer);
        showOutputWindow(context, outputBuffer);

        Path workingDirectory = resolveWorkingDirectory(context);
        ShellOutputBufferHelper.appendText(outputBuffer, "$ " + command + "\n");

        return executor.execute(
                        command,
                        workingDirectory,
                        line -> ShellOutputBufferHelper.appendText(outputBuffer, line + "\n"),
                        line -> ShellOutputBufferHelper.appendStyledText(outputBuffer, line + "\n", FaceName.WARNING))
                .thenAccept(
                        exitCode -> ShellOutputBufferHelper.appendText(outputBuffer, "\nexit code: " + exitCode + "\n"))
                .exceptionally(ex -> {
                    logger().warn("シェルコマンドの実行に失敗", ex);
                    context.handleError("シェルコマンドの実行に失敗: " + ex.getMessage(), ex);
                    return null;
                })
                .whenComplete((result2, ex) -> running.set(false));
    }

    private void showOutputWindow(CommandContext context, BufferFacade outputBuffer) {
        var windows = context.frame().getWindowTree().windows();
        for (var window : windows) {
            if (window.getBuffer().equals(outputBuffer)) {
                context.frame().setActiveWindow(window);
                window.setPoint(0);
                return;
            }
        }
        context.frame().splitActiveWindow(Direction.HORIZONTAL, outputBuffer);
    }

    private static Path resolveWorkingDirectory(CommandContext context) {
        return context.activeWindow()
                .getBuffer()
                .getDefaultDirectory(Path.of("").toAbsolutePath());
    }

    private BufferFacade getOrCreateOutputBuffer(CommandContext context) {
        var existing = context.bufferManager().findByName(OUTPUT_BUFFER_NAME);
        if (existing.isPresent()) {
            return existing.get();
        }
        var textBuffer = new TextBuffer(OUTPUT_BUFFER_NAME, new GapTextModel(), context.settingsRegistry());
        var bufferFacade = new BufferFacade(textBuffer);
        bufferFacade.setReadOnly(true);
        context.bufferManager().add(bufferFacade);
        return bufferFacade;
    }
}
