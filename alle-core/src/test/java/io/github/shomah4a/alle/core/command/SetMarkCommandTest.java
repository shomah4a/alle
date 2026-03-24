package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SetMarkCommandTest {

    @Test
    void コマンド名はsetMarkである() {
        var cmd = new SetMarkCommand();
        assertEquals("set-mark", cmd.name());
    }

    @Test
    void 現在のポイント位置にmarkが設定される() {
        var result = TestCommandContextFactory.createDefaultWithFrame();
        var context = result.context();
        var window = result.frame().getActiveWindow();
        window.insert("Hello");
        window.setPoint(3);

        new SetMarkCommand().execute(context).join();

        assertEquals(3, window.getMark().orElseThrow());
    }

    @Test
    void 先頭でmarkを設定できる() {
        var result = TestCommandContextFactory.createDefaultWithFrame();
        var context = result.context();
        var window = result.frame().getActiveWindow();
        window.insert("Hello");
        window.setPoint(0);

        new SetMarkCommand().execute(context).join();

        assertEquals(0, window.getMark().orElseThrow());
    }

    @Test
    void 末尾でmarkを設定できる() {
        var result = TestCommandContextFactory.createDefaultWithFrame();
        var context = result.context();
        var window = result.frame().getActiveWindow();
        window.insert("Hello");

        new SetMarkCommand().execute(context).join();

        assertEquals(5, window.getMark().orElseThrow());
    }

    @Test
    void markを再設定すると上書きされる() {
        var result = TestCommandContextFactory.createDefaultWithFrame();
        var context = result.context();
        var window = result.frame().getActiveWindow();
        window.insert("Hello");
        window.setPoint(2);
        new SetMarkCommand().execute(context).join();

        window.setPoint(4);
        new SetMarkCommand().execute(context).join();

        assertEquals(4, window.getMark().orElseThrow());
    }
}
