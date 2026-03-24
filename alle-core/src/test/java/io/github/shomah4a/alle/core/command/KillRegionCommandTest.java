package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.TestCommandContextFactory.CreateResult;
import org.junit.jupiter.api.Test;

class KillRegionCommandTest {

    @Test
    void コマンド名はkillRegionである() {
        assertEquals("kill-region", new KillRegionCommand().name());
    }

    @Test
    void markからpointまでのテキストを削除してkillRingに蓄積する() {
        var killRing = new KillRing();
        var result = createContext(killRing);
        var window = result.frame().getActiveWindow();
        window.insert("Hello World");
        window.setMark(5);
        window.setPoint(11);

        new KillRegionCommand().execute(result.context()).join();

        assertEquals("Hello", window.getBuffer().getText());
        assertEquals(5, window.getPoint());
        assertTrue(window.getMark().isEmpty());
        assertEquals(" World", killRing.current().orElseThrow());
    }

    @Test
    void pointがmarkより前の場合も正しく削除する() {
        var killRing = new KillRing();
        var result = createContext(killRing);
        var window = result.frame().getActiveWindow();
        window.insert("Hello World");
        window.setMark(11);
        window.setPoint(5);

        new KillRegionCommand().execute(result.context()).join();

        assertEquals("Hello", window.getBuffer().getText());
        assertEquals(5, window.getPoint());
        assertEquals(" World", killRing.current().orElseThrow());
    }

    @Test
    void markが未設定の場合は何もしない() {
        var killRing = new KillRing();
        var result = createContext(killRing);
        var window = result.frame().getActiveWindow();
        window.insert("Hello");

        new KillRegionCommand().execute(result.context()).join();

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

        new KillRegionCommand().execute(result.context()).join();

        assertEquals("Hello", window.getBuffer().getText());
        assertTrue(killRing.current().isEmpty());
    }

    @Test
    void killRegionの削除をundoで復元できる() {
        var killRing = new KillRing();
        var result = createContext(killRing);
        var window = result.frame().getActiveWindow();
        window.insert("Hello World");
        window.setMark(5);
        window.setPoint(11);

        new KillRegionCommand().execute(result.context()).join();
        assertEquals("Hello", window.getBuffer().getText());

        new UndoCommand().execute(result.context()).join();
        assertEquals("Hello World", window.getBuffer().getText());
    }

    private CreateResult createContext(KillRing killRing) {
        var defaultResult = TestCommandContextFactory.createDefaultWithFrame();
        var context = TestCommandContextFactory.create(
                defaultResult.frame(), defaultResult.context().bufferManager(), killRing, java.util.Optional.empty());
        return new CreateResult(defaultResult.frame(), context);
    }
}
