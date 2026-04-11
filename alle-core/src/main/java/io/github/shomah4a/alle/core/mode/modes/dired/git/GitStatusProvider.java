package io.github.shomah4a.alle.core.mode.modes.dired.git;

import java.nio.file.Path;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;

/**
 * gitリポジトリの情報を取得するインターフェース。
 * 副作用（プロセス実行）を外部化し、テスタビリティを確保する。
 */
public interface GitStatusProvider {

    /**
     * 指定ディレクトリ配下のファイルのgitステータスを取得する。
     * キーはファイルの絶対パス、値はステータス文字列（"M", "A", "?", "D" 等）。
     *
     * @param rootDirectory リポジトリ内のディレクトリ
     * @return パスとステータスのマッピング。git管理外のファイルは含まれない
     */
    MapIterable<Path, String> getFileStatuses(Path rootDirectory);

    /**
     * 現在のブランチ名を取得する。
     * デタッチドHEADの場合は短縮コミットハッシュを返す。
     *
     * @param rootDirectory リポジトリ内のディレクトリ
     * @return ブランチ名。取得できない場合は空文字列
     */
    String getBranch(Path rootDirectory);

    /**
     * 指定ファイルをgitのステージングエリアに追加する。
     *
     * @param rootDirectory リポジトリ内のディレクトリ
     * @param files ステージング対象のファイルパスリスト
     */
    void stageFiles(Path rootDirectory, ListIterable<Path> files);

    /**
     * 指定ファイルがgit管理下かどうかを返す。
     *
     * @param rootDirectory リポジトリ内のディレクトリ
     * @param file 判定対象のファイルパス
     * @return git管理下ならtrue
     */
    boolean isTracked(Path rootDirectory, Path file);

    /**
     * git rm を実行する。
     *
     * @param rootDirectory リポジトリ内のディレクトリ
     * @param files 削除対象のファイルパスリスト
     * @param force ディレクトリの再帰削除を許可するか
     */
    void removeFiles(Path rootDirectory, ListIterable<Path> files, boolean force);

    /**
     * git mv を実行する。
     *
     * @param rootDirectory リポジトリ内のディレクトリ
     * @param source 移動元パス
     * @param destination 移動先パス
     */
    void moveFile(Path rootDirectory, Path source, Path destination);
}
