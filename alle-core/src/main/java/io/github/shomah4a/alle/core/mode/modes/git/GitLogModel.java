package io.github.shomah4a.alle.core.mode.modes.git;

import java.nio.file.Path;
import java.util.Optional;

/**
 * git-log バッファ 1 つに紐づく状態モデル。
 * 追加取得型ページネーションのため、対象パラメータと読み込み済み件数を保持する。
 * loadedCount は次ページ取得時の {@code --skip} の値として使う。
 */
public class GitLogModel {

    private final Path repoRoot;
    private final Optional<Path> target;
    private final int pageSize;
    private int loadedCount;

    public GitLogModel(Path repoRoot, Optional<Path> target, int pageSize) {
        this.repoRoot = repoRoot;
        this.target = target;
        this.pageSize = pageSize;
        this.loadedCount = 0;
    }

    public Path repoRoot() {
        return repoRoot;
    }

    public Optional<Path> target() {
        return target;
    }

    public int pageSize() {
        return pageSize;
    }

    public int loadedCount() {
        return loadedCount;
    }

    public void addLoaded(int count) {
        this.loadedCount += count;
    }
}
