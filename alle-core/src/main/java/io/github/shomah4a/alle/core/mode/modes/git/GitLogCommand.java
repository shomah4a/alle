package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Direction;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * バッファに紐づく Path の git log を別バッファに表示するコマンド。
 * 対象パスは、tree-dired バッファならそのディレクトリ、それ以外は buffer.getFilePath() から取る。
 * リポジトリルート探索は GitRepositoryLocator に委譲する。
 * subprocess 実行は共有 ForkJoinPool 上で非同期に行い、UI スレッドをブロックしない。
 * 追加取得型ページネーションのため、結果バッファには GitLogMode + GitLogModel を紐付ける。
 */
public class GitLogCommand implements Command {

    private final GitLogProvider provider;
    private final GitRepositoryLocator locator;
    private final Keymap gitLogKeymap;
    private final CommandRegistry gitLogCommandRegistry;
    private final SettingsRegistry settingsRegistry;

    public GitLogCommand(
            GitLogProvider provider,
            GitRepositoryLocator locator,
            Keymap gitLogKeymap,
            CommandRegistry gitLogCommandRegistry,
            SettingsRegistry settingsRegistry) {
        this.provider = provider;
        this.locator = locator;
        this.gitLogKeymap = gitLogKeymap;
        this.gitLogCommandRegistry = gitLogCommandRegistry;
        this.settingsRegistry = settingsRegistry;
    }

    @Override
    public String name() {
        return "git-log";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        BufferFacade sourceBuffer = context.activeWindow().getBuffer();
        Optional<Path> targetPathOpt = resolveTargetPath(sourceBuffer);
        if (targetPathOpt.isEmpty()) {
            context.messageBuffer().message("git-log: バッファに紐づく Path がありません");
            return CompletableFuture.completedFuture(null);
        }
        Path targetPath = targetPathOpt.get();
        Optional<Path> repoRootOpt = locator.locate(targetPath);
        if (repoRootOpt.isEmpty()) {
            context.messageBuffer().message("git-log: git リポジトリ配下ではありません");
            return CompletableFuture.completedFuture(null);
        }
        Path repoRoot = repoRootOpt.get();

        var settings = sourceBuffer.getSettings();
        int pageSize = settings.get(GitSettings.LOG_PAGE_SIZE);
        int subjectMaxWidth = settings.get(GitSettings.LOG_SUBJECT_MAX_WIDTH);
        int bodyMaxLines = settings.get(GitSettings.LOG_BODY_MAX_LINES);

        Kind kind = classify(sourceBuffer, targetPath, repoRoot);
        Optional<Path> logTarget = kind == Kind.REPO ? Optional.empty() : Optional.of(targetPath);
        String bufferName = buildBufferName(repoRoot, targetPath, kind);
        var config = new GitLogRenderer.RenderConfig(subjectMaxWidth, bodyMaxLines, ZoneId.systemDefault());

        // 同期パート: Frame / BufferManager の変更はコマンドループスレッドで完結させる。
        // 非同期化してよいのは subprocess 呼び出しとバッファテキストへの書き込みのみ。
        var prepared = prepareLogBuffer(context, bufferName, repoRoot, logTarget, pageSize);

        // 非同期パート: subprocess でログを取得し、バッファへ書き込む。
        return CompletableFuture.supplyAsync(() -> provider.getLog(repoRoot, logTarget, 0, pageSize))
                .thenAccept(entries -> renderEntries(prepared, entries, config));
    }

    private Optional<Path> resolveTargetPath(BufferFacade buffer) {
        MajorMode majorMode = buffer.getMajorMode();
        if (majorMode instanceof TreeDiredMode diredMode) {
            return Optional.of(diredMode.getModel().getRootDirectory());
        }
        return buffer.getFilePath();
    }

    private Kind classify(BufferFacade buffer, Path targetPath, Path repoRoot) {
        if (targetPath.equals(repoRoot)) {
            return Kind.REPO;
        }
        if (buffer.getMajorMode() instanceof TreeDiredMode) {
            return Kind.DIR;
        }
        return Kind.FILE;
    }

    private String buildBufferName(Path repoRoot, Path targetPath, Kind kind) {
        String relative =
                kind == Kind.REPO ? "." : repoRoot.relativize(targetPath).toString();
        return "*git-log: " + relative + " [" + kind.label + "]*";
    }

    /**
     * 結果バッファ (新規 or 既存) を用意し、GitLogMode を紐付け、split 表示までを同期で完了する。
     * Frame / BufferManager への書き込みは全てここで済ませ、非同期パートには渡さない。
     */
    private PreparedBuffer prepareLogBuffer(
            CommandContext context, String bufferName, Path repoRoot, Optional<Path> logTarget, int pageSize) {

        var existingOpt = context.bufferManager().findByName(bufferName);
        BufferFacade logBuffer;
        boolean isNew;
        if (existingOpt.isPresent()) {
            logBuffer = existingOpt.get();
            isNew = false;
        } else {
            var textBuffer = new TextBuffer(bufferName, new GapTextModel(), settingsRegistry);
            logBuffer = new BufferFacade(textBuffer);
            isNew = true;
        }

        // 新しい GitLogModel と GitLogMode を割り当てる。既存バッファでもモード付け替えで
        // 前回のページネーション状態をリセットする。loadedCount は非同期で render 後に加算する。
        var model = new GitLogModel(repoRoot, logTarget, pageSize);
        var mode = new GitLogMode(model, gitLogKeymap, gitLogCommandRegistry);
        // git-log バッファ自身は git リポジトリ配下判定の対象にしないため、
        // runMajorModeHooks は意図的にスキップする。切替側から hook を呼ぶ既存パターンは
        // docs/tasks.md「モード切替時の hook 呼び出しを内部化する」で別タスク化した。
        logBuffer.setMajorMode(mode);

        // 前回内容が残っている場合は空にする。
        logBuffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            int len = buf.length();
            if (len > 0) {
                buf.deleteText(0, len);
            }
            buf.markClean();
            buf.setReadOnly(true);
            return null;
        });

        if (isNew) {
            context.bufferManager().add(logBuffer);
            context.frame().splitActiveWindow(Direction.HORIZONTAL, logBuffer);
        } else {
            var windows = context.frame().getWindowTree().windows();
            boolean focused = false;
            for (var window : windows) {
                if (window.getBuffer().equals(logBuffer)) {
                    context.frame().setActiveWindow(window);
                    window.setPoint(0);
                    focused = true;
                    break;
                }
            }
            if (!focused) {
                context.frame().splitActiveWindow(Direction.HORIZONTAL, logBuffer);
            }
        }

        return new PreparedBuffer(logBuffer, model);
    }

    private void renderEntries(
            PreparedBuffer prepared, ImmutableList<GitLogEntry> entries, GitLogRenderer.RenderConfig config) {
        prepared.model.addLoaded(entries.size());
        prepared.buffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            GitLogRenderer.render(buf, entries, config);
            buf.markClean();
            buf.setReadOnly(true);
            return null;
        });
    }

    private record PreparedBuffer(BufferFacade buffer, GitLogModel model) {}

    private enum Kind {
        REPO("repo"),
        DIR("dir"),
        FILE("file");

        private final String label;

        Kind(String label) {
            this.label = label;
        }
    }
}
