package io.github.shomah4a.alle.core.mode.modes.dired;

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
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.list.ListIterable;

/**
 * マーク済みまたはカーソル行のファイルパスをシェルコマンドに渡して実行する。
 *
 * <p>コマンド文字列の置換ルール:
 * <ul>
 *   <li>{@code *} を含む場合: 対象ファイルパスのスペース区切りリストに置換</li>
 *   <li>{@code ?} を含む場合: ファイルごとに個別実行し、{@code ?} をファイルパスに置換</li>
 *   <li>どちらも含まない場合: コマンド末尾にファイルパスをスペース区切りで追加</li>
 *   <li>{@code *} と {@code ?} の両方を含む場合: エラー</li>
 * </ul>
 *
 * <p>ファイルパスはシングルクォートでエスケープされ、スペースや特殊文字を含むパスでも安全に扱われる。
 */
public class TreeDiredShellCommand implements Command, Loggable {

    private static final String OUTPUT_BUFFER_NAME = "*Shell Command Output*";

    private final ShellCommandExecutor executor;
    private final InputHistory shellHistory;

    public TreeDiredShellCommand(ShellCommandExecutor executor, InputHistory shellHistory) {
        this.executor = executor;
        this.shellHistory = shellHistory;
    }

    @Override
    public String name() {
        return "tree-dired-shell-command";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        var mode = context.activeWindow().getBuffer().getMajorMode();
        if (!(mode instanceof TreeDiredMode diredMode)) {
            return CompletableFuture.completedFuture(null);
        }

        ListIterable<TreeDiredEntry> targets = TreeDiredEntryResolver.resolveTargets(context.activeWindow(), diredMode);
        if (targets.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        String prompt = buildPrompt(targets);

        return context.inputPrompter()
                .prompt(prompt, shellHistory)
                .thenCompose(result -> handlePromptResult(context, diredMode, targets, result));
    }

    private static String buildPrompt(ListIterable<TreeDiredEntry> targets) {
        if (targets.size() == 1) {
            return "Shell command on " + targets.get(0).path().getFileName() + ": ";
        }
        return "Shell command on " + targets.size() + " files: ";
    }

    private CompletableFuture<Void> handlePromptResult(
            CommandContext context,
            TreeDiredMode diredMode,
            ListIterable<TreeDiredEntry> targets,
            PromptResult result) {
        if (!(result instanceof PromptResult.Confirmed confirmed)) {
            return CompletableFuture.completedFuture(null);
        }
        String commandTemplate = confirmed.value().trim();
        if (commandTemplate.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        boolean hasStar = commandTemplate.contains("*");
        boolean hasQuestion = commandTemplate.contains("?");
        if (hasStar && hasQuestion) {
            context.messageBuffer().message("* と ? は同時に使用できません");
            return CompletableFuture.completedFuture(null);
        }

        ListIterable<String> quotedPaths =
                targets.collect(e -> shellQuote(e.path().toString()));
        Path workingDirectory = resolveWorkingDirectory(targets, diredMode);

        diredMode.getModel().clearMarks();
        TreeDiredBufferUpdater.update(context.activeWindow(), diredMode);

        var outputBuffer = getOrCreateOutputBuffer(context);
        ShellOutputBufferHelper.clearBuffer(outputBuffer);
        context.frame().getActiveWindow().setBuffer(outputBuffer);

        return executeAndStream(commandTemplate, quotedPaths, workingDirectory, hasStar, hasQuestion, outputBuffer)
                .exceptionally(ex -> {
                    logger().warn("シェルコマンドの実行に失敗", ex);
                    context.handleError("シェルコマンドの実行に失敗: " + ex.getMessage(), ex);
                    return null;
                });
    }

    private CompletableFuture<Void> executeAndStream(
            String commandTemplate,
            ListIterable<String> quotedPaths,
            Path workingDirectory,
            boolean hasStar,
            boolean hasQuestion,
            BufferFacade outputBuffer) {
        if (hasQuestion) {
            return executePerFileStreaming(commandTemplate, quotedPaths, workingDirectory, outputBuffer);
        }
        String fileArgs = String.join(" ", quotedPaths.toList());
        String cmd;
        if (hasStar) {
            cmd = commandTemplate.replace("*", fileArgs);
        } else {
            cmd = commandTemplate + " " + fileArgs;
        }
        return executeSingleStreaming(cmd, workingDirectory, outputBuffer);
    }

    private CompletableFuture<Void> executeSingleStreaming(
            String cmd, Path workingDirectory, BufferFacade outputBuffer) {
        ShellOutputBufferHelper.appendText(outputBuffer, "$ " + cmd + "\n");
        return executor.execute(
                        cmd,
                        workingDirectory,
                        line -> ShellOutputBufferHelper.appendText(outputBuffer, line + "\n"),
                        line -> ShellOutputBufferHelper.appendStyledText(outputBuffer, line + "\n", FaceName.WARNING))
                .thenAccept(
                        exitCode -> ShellOutputBufferHelper.appendText(outputBuffer, "exit code: " + exitCode + "\n"));
    }

    private CompletableFuture<Void> executePerFileStreaming(
            String commandTemplate,
            ListIterable<String> quotedPaths,
            Path workingDirectory,
            BufferFacade outputBuffer) {
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (int i = 0; i < quotedPaths.size(); i++) {
            String quotedPath = quotedPaths.get(i);
            String cmd = commandTemplate.replace("?", quotedPath);
            boolean needsSeparator = i > 0;
            chain = chain.thenCompose(ignored -> {
                if (needsSeparator) {
                    ShellOutputBufferHelper.appendText(outputBuffer, "\n");
                }
                return executeSingleStreaming(cmd, workingDirectory, outputBuffer);
            });
        }
        return chain;
    }

    /**
     * シェル用にファイルパスをシングルクォートでエスケープする。
     * パス中のシングルクォートは {@code '\''} に置換する。
     */
    static String shellQuote(String path) {
        return "'" + path.replace("'", "'\\''") + "'";
    }

    private static Path resolveWorkingDirectory(ListIterable<TreeDiredEntry> targets, TreeDiredMode diredMode) {
        Path firstParent = targets.get(0).path().getParent();
        if (firstParent != null) {
            return firstParent;
        }
        return diredMode.getModel().getRootDirectory();
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
