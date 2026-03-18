package io.github.shomah4a.alle.core.command;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * 終了処理ハンドラの登録と実行を管理する。
 * 優先度（小さいほど先に実行）付きでハンドラを登録し、
 * executeAll()で優先度順に順次実行する。
 * ハンドラがfalseを返すと以降のハンドラを実行せず終了を中断する。
 */
public class ShutdownHandler {

    private final MutableList<Entry> entries = Lists.mutable.empty();

    /**
     * 終了処理ハンドラを優先度付きで登録する。
     *
     * @param priority 優先度（小さいほど先に実行）
     * @param handler trueで続行、falseで終了中断
     */
    public void register(int priority, Supplier<CompletableFuture<Boolean>> handler) {
        entries.add(new Entry(priority, handler));
        entries.sortThisBy(Entry::priority);
    }

    /**
     * 登録されたハンドラを優先度順に順次実行する。
     * 全ハンドラがtrueを返した場合はtrueを返す。
     * いずれかのハンドラがfalseを返した場合、以降のハンドラは実行せずfalseを返す。
     */
    public CompletableFuture<Boolean> executeAll() {
        CompletableFuture<Boolean> future = CompletableFuture.completedFuture(true);
        for (var entry : entries) {
            future = future.thenCompose(proceed -> {
                if (!proceed) {
                    return CompletableFuture.completedFuture(false);
                }
                return entry.handler().get();
            });
        }
        return future;
    }

    private record Entry(int priority, Supplier<CompletableFuture<Boolean>> handler) {}
}
