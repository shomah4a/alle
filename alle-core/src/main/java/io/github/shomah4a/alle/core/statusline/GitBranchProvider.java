package io.github.shomah4a.alle.core.statusline;

import java.nio.file.Path;
import java.util.Optional;

/**
 * gitブランチ情報を提供するインターフェース。
 * 外部プロセス呼び出しを抽象化し、テスタビリティを確保する。
 */
public interface GitBranchProvider {

    /**
     * 指定パスが属するgitリポジトリのブランチ情報を返す。
     * git管理外の場合はemptyを返す。
     *
     * @param filePath バッファに関連付けられたファイルパス
     */
    Optional<GitBranchInfo> getBranch(Path filePath);

    /**
     * gitブランチ情報。
     *
     * @param branchName ブランチ名（detached HEADの場合は短縮コミットハッシュ）
     * @param dirty ワーキングツリーに未コミットの変更があるかどうか
     */
    record GitBranchInfo(String branchName, boolean dirty) {}
}
