package io.github.shomah4a.alle.core.input;

import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.collections.api.list.ListIterable;

/**
 * ディレクトリ内のエントリ一覧を文字列として取得する。
 * ファイルシステムアクセスの副作用を外部化するためのインターフェース。
 * ディレクトリエントリにはパス区切り文字 "/" を付与して返す。
 */
@FunctionalInterface
public interface DirectoryLister {

    /**
     * 指定ディレクトリ内のエントリ一覧を返す。
     * ディレクトリエントリには末尾に "/" を付与する。
     *
     * @param directory 対象ディレクトリ
     * @return エントリのパス文字列一覧
     * @throws IOException 読み取りに失敗した場合
     */
    ListIterable<String> list(Path directory) throws IOException;
}
