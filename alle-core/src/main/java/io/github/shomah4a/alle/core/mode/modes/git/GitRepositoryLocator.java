package io.github.shomah4a.alle.core.mode.modes.git;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

/**
 * Git リポジトリルート探索器。
 * Path (file または dir) を渡すと親を遡って .git を持つディレクトリを返す。
 * git worktree の .git がファイルであっても Files.exists で存在検知する。
 * 探索結果は指定 TTL / サイズの Caffeine キャッシュに保持し、
 * 自動有効化 hook から高頻度で呼ばれる用途で FS stat のコストを吸収する。
 */
public class GitRepositoryLocator {

    private static final Duration DEFAULT_TTL = Duration.ofSeconds(5);
    private static final int DEFAULT_MAX_SIZE = 100;

    private final Cache<Path, Optional<Path>> cache;
    private final Function<Path, Optional<Path>> resolver;

    public GitRepositoryLocator() {
        this(DEFAULT_TTL, DEFAULT_MAX_SIZE, GitRepositoryLocator::resolveUncached);
    }

    GitRepositoryLocator(Duration ttl, int maxSize, Function<Path, Optional<Path>> resolver) {
        this.cache =
                Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
        this.resolver = resolver;
    }

    /**
     * 入力 Path から git リポジトリルートを探索する。
     * 引数 Path 自身から親方向に .git を持つディレクトリを探す。
     * 見つからなければ empty。
     */
    public Optional<Path> locate(Path path) {
        return cache.get(path, resolver);
    }

    private static Optional<Path> resolveUncached(Path start) {
        Path dir = start;
        while (dir != null) {
            if (Files.exists(dir.resolve(".git"))) {
                return Optional.of(dir);
            }
            dir = dir.getParent();
        }
        return Optional.empty();
    }
}
