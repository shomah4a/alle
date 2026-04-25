package io.github.shomah4a.alle.core.statusline;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * GitBranchProviderのキャッシュデコレータ。
 * gitリポジトリルート単位でブランチ情報をキャッシュする。
 * キャッシュキーはファイルパスの親ディレクトリ（簡易的にリポジトリルートの近似として使用）。
 */
public class CachingGitBranchProvider implements GitBranchProvider {

    private final GitBranchProvider delegate;
    private final Cache<Path, Optional<GitBranchInfo>> cache;
    private final Cache<Path, Optional<Path>> gitRootCache;
    private final GitRootResolver gitRootResolver;

    /**
     * デフォルトのTTLとキャッシュサイズで構築する。
     */
    public CachingGitBranchProvider(GitBranchProvider delegate) {
        this(delegate, Duration.ofSeconds(5), 100, CachingGitBranchProvider::resolveGitRoot);
    }

    CachingGitBranchProvider(GitBranchProvider delegate, Duration ttl, int maxSize, GitRootResolver gitRootResolver) {
        this.delegate = delegate;
        this.cache =
                Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
        this.gitRootCache =
                Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
        this.gitRootResolver = gitRootResolver;
    }

    @Override
    public Optional<GitBranchInfo> getBranch(Path filePath) {
        Path parent = filePath.getParent();
        if (parent == null) {
            return Optional.empty();
        }
        Optional<Path> gitRoot = gitRootCache.get(parent, k -> Optional.ofNullable(gitRootResolver.resolve(filePath)));
        if (gitRoot.isEmpty()) {
            return Optional.empty();
        }
        return cache.get(gitRoot.get(), k -> delegate.getBranch(filePath));
    }

    private static @Nullable Path resolveGitRoot(Path filePath) {
        @Nullable Path dir = filePath.getParent();
        while (dir != null) {
            if (dir.resolve(".git").toFile().exists()) {
                return dir;
            }
            dir = dir.getParent();
        }
        // .gitが見つからなかった場合はnullを返し、キャッシュをスキップする
        return null;
    }

    /**
     * gitリポジトリルートを解決する関数型インターフェース。テスト時にモックを注入するために使用する。
     */
    @FunctionalInterface
    interface GitRootResolver {
        @Nullable
        Path resolve(Path filePath);
    }
}
