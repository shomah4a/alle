package io.github.shomah4a.alle.core.statusline;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.shomah4a.alle.core.mode.modes.git.GitRepositoryLocator;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;

/**
 * GitBranchProviderのキャッシュデコレータ。
 * gitリポジトリルート単位でブランチ情報をキャッシュする。
 * リポジトリルート探索は GitRepositoryLocator に委譲する。
 */
public class CachingGitBranchProvider implements GitBranchProvider {

    private final GitBranchProvider delegate;
    private final Cache<Path, Optional<GitBranchInfo>> cache;
    private final GitRepositoryLocator locator;

    /**
     * デフォルトのTTLとキャッシュサイズで構築する。
     */
    public CachingGitBranchProvider(GitBranchProvider delegate) {
        this(delegate, Duration.ofSeconds(5), 100, new GitRepositoryLocator());
    }

    /**
     * 外部から共有 Locator を注入して構築する。git-mode の自動有効化 hook と
     * リポジトリルート探索キャッシュを一本化する用途で使う。
     */
    public CachingGitBranchProvider(GitBranchProvider delegate, GitRepositoryLocator locator) {
        this(delegate, Duration.ofSeconds(5), 100, locator);
    }

    CachingGitBranchProvider(GitBranchProvider delegate, Duration ttl, int maxSize, GitRepositoryLocator locator) {
        this.delegate = delegate;
        this.cache =
                Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
        this.locator = locator;
    }

    @Override
    public Optional<GitBranchInfo> getBranch(Path filePath) {
        Optional<Path> gitRoot = locator.locate(filePath);
        if (gitRoot.isEmpty()) {
            return Optional.empty();
        }
        return cache.get(gitRoot.get(), k -> delegate.getBranch(filePath));
    }
}
