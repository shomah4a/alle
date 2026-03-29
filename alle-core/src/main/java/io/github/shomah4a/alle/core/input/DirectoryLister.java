package io.github.shomah4a.alle.core.input;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.collections.api.list.ListIterable;

/**
 * ディレクトリ内のエントリ一覧を取得する。
 * ファイルシステムアクセスの副作用を外部化するためのインターフェース。
 */
@FunctionalInterface
public interface DirectoryLister {

    /**
     * 指定ディレクトリ内のエントリ一覧を返す。
     *
     * @param directory 対象ディレクトリ
     * @return エントリ一覧
     * @throws IOException 読み取りに失敗した場合
     */
    ListIterable<DirectoryEntry> list(Path directory) throws IOException;
}
