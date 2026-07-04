package io.github.shomah4a.alle.core.mode.modes.git;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * git-log バッファの末尾に追加ページを取得して追記するコマンド。
 * 追加取得型ページネーションを実現する。
 */
public class GitLogNextPageCommand implements Command {

    private final GitLogProvider provider;

    public GitLogNextPageCommand(GitLogProvider provider) {
        this.provider = provider;
    }

    @Override
    public String name() {
        return "git-log-next-page";
    }

    @Override
    public CompletableFuture<Void> execute(CommandContext context) {
        BufferFacade buffer = context.activeWindow().getBuffer();
        if (!(buffer.getMajorMode() instanceof GitLogMode mode)) {
            context.messageBuffer().message("git-log-next-page: git-log バッファではありません");
            return CompletableFuture.completedFuture(null);
        }
        var model = mode.getModel();
        int skip = model.loadedCount();
        int pageSize = model.pageSize();
        var settings = buffer.getSettings();
        int subjectMaxWidth = settings.get(GitSettings.LOG_SUBJECT_MAX_WIDTH);
        int bodyMaxLines = settings.get(GitSettings.LOG_BODY_MAX_LINES);
        var config = new GitLogRenderer.RenderConfig(subjectMaxWidth, bodyMaxLines, ZoneId.systemDefault());

        return CompletableFuture.supplyAsync(() -> provider.getLog(model.repoRoot(), model.target(), skip, pageSize))
                .thenAccept(entries -> appendToBuffer(buffer, model, entries, config));
    }

    private void appendToBuffer(
            BufferFacade buffer,
            GitLogModel model,
            ImmutableList<GitLogEntry> entries,
            GitLogRenderer.RenderConfig config) {
        if (entries.isEmpty()) {
            return;
        }
        model.addLoaded(entries.size());
        buffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            GitLogRenderer.appendEntries(buf, entries, config);
            buf.markClean();
            buf.setReadOnly(true);
            return null;
        });
    }
}
