package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.TestCommandContextFactory.CreateResult;
import org.junit.jupiter.api.Test;

class CopyRegionCommandTest {

    @Test
    void コマンド名はcopyRegionである() {
        assertEquals("copy-region", new CopyRegionCommand().name());
    }

    @Test
    void markからpointまでのテキストをkillRingにコピーしてテキストは残る() {
        var killRing = new KillRing();
        var result = createContext(killRing);
        var window = result.frame().getActiveWindow();
        window.insert("Hello World");
        window.setMark(6);
        window.setPoint(11);

        new CopyRegionCommand().execute(result.context()).join();

        assertEquals("Hello World", window.getBuffer().getText());
        assertEquals(11, window.getPoint());
        assertTrue(window.getMark().isEmpty());
        assertEquals("World", killRing.current().orElseThrow());
    }

    @Test
    void markが未設定の場合は何もしない() {
        var killRing = new KillRing();
        var result = createContext(killRing);
        var window = result.frame().getActiveWindow();
        window.insert("Hello");

        new CopyRegionCommand().execute(result.context()).join();

        assertEquals("Hello", window.getBuffer().getText());
        assertTrue(killRing.current().isEmpty());
    }

    @Test
    void markとpointが同じ位置の場合は何もしない() {
        var killRing = new KillRing();
        var result = createContext(killRing);
        var window = result.frame().getActiveWindow();
        window.insert("Hello");
        window.setMark(3);
        window.setPoint(3);

        new CopyRegionCommand().execute(result.context()).join();

        assertEquals("Hello", window.getBuffer().getText());
        assertTrue(killRing.current().isEmpty());
    }

    private CreateResult createContext(KillRing killRing) {
        var defaultResult = TestCommandContextFactory.createDefaultWithFrame();
        var context = TestCommandContextFactory.create(
                defaultResult.frame(), defaultResult.context().bufferManager(), killRing, java.util.Optional.empty());
        return new CreateResult(defaultResult.frame(), context);
    }
}
