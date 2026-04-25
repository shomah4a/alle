package io.github.shomah4a.alle.core.server;

import java.nio.file.Path;
import java.util.Optional;

/**
 * ファイルパスから ServerSession を検索するインターフェース。
 * ServerManager が実装し、ServerEditCommand から利用する。
 */
public interface ServerSessionLookup {

    /**
     * 指定パスに紐づくセッションを検索する。
     */
    Optional<ServerSession> findByPath(Path path);

    /**
     * 指定パスに紐づくセッションを除去する。
     */
    void removeByPath(Path path);
}
