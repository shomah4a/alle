package io.github.shomah4a.alle.core.mode.modes.git;

import java.nio.file.Path;
import java.util.Optional;
import org.eclipse.collections.api.list.ImmutableList;

/**
 * git log を取得する抽象。
 * subprocess 実装のほか、テストではスタブに差し替える。
 */
public interface GitLogProvider {

    /**
     * リポジトリの git log を取得する。
     *
     * @param repoRoot git コマンドの実行対象ディレクトリ (リポジトリルート)
     * @param target 履歴対象のファイルまたはディレクトリ。empty ならリポジトリ全体
     * @param maxCount 取得件数上限
     * @return コミットの新しい順 (git log のデフォルト順)。取得失敗時は空
     */
    ImmutableList<GitLogEntry> getLog(Path repoRoot, Optional<Path> target, int maxCount);
}
