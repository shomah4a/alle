package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.input.ShutdownRequestable;
import org.junit.jupiter.api.Test;

class ProcessQuitCommandTest {

    @Test
    void executeでshutdownが要求される() {
        var requestable = new TestShutdownRequestable();
        var command = new ProcessQuitCommand(requestable);
        var context = TestCommandContextFactory.createDefault();

        command.execute(context).join();

        assertTrue(requestable.shutdownRequested);
    }

    @Test
    void コマンド名はquit() {
        var requestable = new TestShutdownRequestable();
        var command = new ProcessQuitCommand(requestable);

        assertEquals("quit", command.name());
    }

    private static class TestShutdownRequestable implements ShutdownRequestable {
        boolean shutdownRequested = false;

        @Override
        public void requestShutdown() {
            shutdownRequested = true;
        }
    }
}
