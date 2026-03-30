package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.ShutdownHandler;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.input.ShutdownRequestable;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class SaveBuffersKillAlleCommandTest {

    @Test
    void 全ハンドラ通過でshutdownが呼ばれる() {
        var shutdownHandler = new ShutdownHandler();
        var requestable = new TestShutdownRequestable();
        var command = new SaveBuffersKillAlleCommand(shutdownHandler, requestable);
        var context = TestCommandContextFactory.createDefault();

        command.execute(context).join();

        assertTrue(requestable.shutdownRequested);
    }

    @Test
    void ハンドラがfalseを返すとshutdownが呼ばれない() {
        var shutdownHandler = new ShutdownHandler();
        shutdownHandler.register(10, () -> CompletableFuture.completedFuture(false));
        var requestable = new TestShutdownRequestable();
        var command = new SaveBuffersKillAlleCommand(shutdownHandler, requestable);
        var context = TestCommandContextFactory.createDefault();

        command.execute(context).join();

        assertFalse(requestable.shutdownRequested);
    }

    @Test
    void ハンドラが一部trueでも途中でfalseならshutdownが呼ばれない() {
        var shutdownHandler = new ShutdownHandler();
        shutdownHandler.register(10, () -> CompletableFuture.completedFuture(true));
        shutdownHandler.register(20, () -> CompletableFuture.completedFuture(false));
        var requestable = new TestShutdownRequestable();
        var command = new SaveBuffersKillAlleCommand(shutdownHandler, requestable);
        var context = TestCommandContextFactory.createDefault();

        command.execute(context).join();

        assertFalse(requestable.shutdownRequested);
    }

    private static class TestShutdownRequestable implements ShutdownRequestable {
        boolean shutdownRequested = false;

        @Override
        public void requestShutdown() {
            shutdownRequested = true;
        }
    }
}
