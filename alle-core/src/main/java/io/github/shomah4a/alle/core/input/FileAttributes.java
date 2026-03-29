package io.github.shomah4a.alle.core.input;

import java.time.Instant;

/**
 * ファイルまたはディレクトリの属性情報。
 *
 * @param permissions POSIX パーミッション文字列 (例: "rwxr-xr-x")
 * @param linkCount ハードリンク数
 * @param owner オーナー名
 * @param group グループ名
 * @param size バイト単位のサイズ
 * @param lastModified 最終更新日時
 */
public record FileAttributes(
        String permissions, int linkCount, String owner, String group, long size, Instant lastModified) {

    /**
     * テスト用のダミー属性。
     */
    public static final FileAttributes EMPTY = new FileAttributes("", 0, "", "", 0, Instant.EPOCH);
}
