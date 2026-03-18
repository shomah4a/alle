package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Test;

class ShutdownHandlerTest {

    @Test
    void ハンドラ未登録の場合trueを返す() {
        var handler = new ShutdownHandler();
        assertTrue(handler.executeAll().join());
    }

    @Test
    void 全ハンドラがtrueを返すとtrueを返す() {
        var handler = new ShutdownHandler();
        handler.register(10, () -> CompletableFuture.completedFuture(true));
        handler.register(20, () -> CompletableFuture.completedFuture(true));
        assertTrue(handler.executeAll().join());
    }

    @Test
    void ハンドラがfalseを返すとfalseを返す() {
        var handler = new ShutdownHandler();
        handler.register(10, () -> CompletableFuture.completedFuture(false));
        assertFalse(handler.executeAll().join());
    }

    @Test
    void falseを返したハンドラ以降は実行されない() {
        var handler = new ShutdownHandler();
        MutableList<String> executionLog = Lists.mutable.empty();

        handler.register(10, () -> {
            executionLog.add("first");
            return CompletableFuture.completedFuture(false);
        });
        handler.register(20, () -> {
            executionLog.add("second");
            return CompletableFuture.completedFuture(true);
        });

        assertFalse(handler.executeAll().join());
        assertEquals(Lists.mutable.of("first"), executionLog);
    }

    @Test
    void 優先度の小さい順に実行される() {
        var handler = new ShutdownHandler();
        MutableList<String> executionLog = Lists.mutable.empty();

        handler.register(30, () -> {
            executionLog.add("third");
            return CompletableFuture.completedFuture(true);
        });
        handler.register(10, () -> {
            executionLog.add("first");
            return CompletableFuture.completedFuture(true);
        });
        handler.register(20, () -> {
            executionLog.add("second");
            return CompletableFuture.completedFuture(true);
        });

        assertTrue(handler.executeAll().join());
        assertEquals(Lists.mutable.of("first", "second", "third"), executionLog);
    }

    @Test
    void 同一優先度のハンドラは登録順に実行される() {
        var handler = new ShutdownHandler();
        MutableList<String> executionLog = Lists.mutable.empty();

        handler.register(10, () -> {
            executionLog.add("a");
            return CompletableFuture.completedFuture(true);
        });
        handler.register(10, () -> {
            executionLog.add("b");
            return CompletableFuture.completedFuture(true);
        });

        assertTrue(handler.executeAll().join());
        assertEquals(Lists.mutable.of("a", "b"), executionLog);
    }
}
