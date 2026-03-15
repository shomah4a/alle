package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;

/**
 * エディタの操作単位。
 * 名前を持ち、コンテキストを受け取って実行する。
 */
public interface Command {

    /**
     * コマンド名を返す。
     */
    String name();

    /**
     * コマンドを実行する。
     */
    CompletableFuture<Void> execute(CommandContext context);
}
