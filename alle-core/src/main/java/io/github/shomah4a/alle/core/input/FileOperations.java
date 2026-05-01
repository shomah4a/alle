package io.github.shomah4a.alle.core.input;

import java.io.IOException;
import java.nio.file.Path;

/**
 * ファイルシステム操作の副作用を外部化するためのインターフェース。
 * コピー・移動・削除・権限変更等のファイル操作を抽象化する。
 */
public interface FileOperations {

    /**
     * ファイルまたはディレクトリをコピーする。
     * ディレクトリの場合は再帰的にコピーする。
     *
     * @param source コピー元パス
     * @param target コピー先パス
     * @throws IOException コピーに失敗した場合
     */
    void copy(Path source, Path target) throws IOException;

    /**
     * ファイルまたはディレクトリを移動（リネーム）する。
     *
     * @param source 移動元パス
     * @param target 移動先パス
     * @throws IOException 移動に失敗した場合
     */
    void move(Path source, Path target) throws IOException;

    /**
     * ファイルまたはディレクトリを削除する。
     * ディレクトリの場合は再帰的に削除する。
     *
     * @param path 削除対象パス
     * @throws IOException 削除に失敗した場合
     */
    void delete(Path path) throws IOException;

    /**
     * ファイルまたはディレクトリのオーナーを変更する。
     *
     * @param path 対象パス
     * @param owner 新しいオーナー名
     * @throws IOException 変更に失敗した場合
     */
    void setOwner(Path path, String owner) throws IOException;

    /**
     * ファイルまたはディレクトリのパーミッションを変更する。
     *
     * @param path 対象パス
     * @param permissions パーミッション文字列（例: "rwxr-xr-x"）
     * @throws IOException 変更に失敗した場合
     */
    void setPermissions(Path path, String permissions) throws IOException;

    /**
     * ディレクトリを再帰的に作成する。
     * 親ディレクトリが存在しない場合は合わせて作成する（mkdir -p 相当）。
     * 既にディレクトリが存在する場合は何もしない。
     *
     * @param path 作成するディレクトリのパス
     * @throws IOException 作成に失敗した場合
     */
    void createDirectories(Path path) throws IOException;
}
