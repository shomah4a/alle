package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class YankCommandTest {

    @Test
    void コマンド名はyankである() {
        assertEquals("yank", new YankCommand().name());
    }

    @Test
    void killRingの最新エントリを挿入する() {
        var killRing = new KillRing();
        killRing.push("World");
        var context = createContext(killRing);
        var window = context.frame().getActiveWindow();
        window.insert("Hello ");

        new YankCommand().execute(context).join();

        assertEquals("Hello World", window.getBuffer().getText());
        assertEquals(11, window.getPoint());
    }

    @Test
    void killRingが空の場合は何もしない() {
        var killRing = new KillRing();
        var context = createContext(killRing);
        var window = context.frame().getActiveWindow();
        window.insert("Hello");

        new YankCommand().execute(context).join();

        assertEquals("Hello", window.getBuffer().getText());
        assertEquals(5, window.getPoint());
    }

    @Test
    void カーソル中間位置で挿入できる() {
        var killRing = new KillRing();
        killRing.push("Beautiful ");
        var context = createContext(killRing);
        var window = context.frame().getActiveWindow();
        window.insert("Hello World");
        window.setPoint(6);

        new YankCommand().execute(context).join();

        assertEquals("Hello Beautiful World", window.getBuffer().getText());
        assertEquals(16, window.getPoint());
    }

    private CommandContext createContext(KillRing killRing) {
        var defaultCtx = TestCommandContextFactory.createDefault();
        return TestCommandContextFactory.create(
                defaultCtx.frame(), defaultCtx.bufferManager(), killRing, Optional.empty());
    }
}
