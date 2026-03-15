package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NextLineCommandTest {

    private Frame frame;
    private BufferManager bufferManager;
    private NextLineCommand cmd;

    @BeforeEach
    void setUp() {
        var buffer = new Buffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new Buffer("*Minibuffer*", new GapTextModel()));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
        cmd = new NextLineCommand();
    }

    @Test
    void 次の行の同じカラム位置に移動する() {
        frame.getActiveWindow().insert("abc\ndef\nghi");
        frame.getActiveWindow().setPoint(1); // 1行目の'b'

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(5, frame.getActiveWindow().getPoint()); // 2行目の'e'
    }

    @Test
    void 次の行が短い場合は行末に移動する() {
        frame.getActiveWindow().insert("abcdef\ngh\nijklmn");
        frame.getActiveWindow().setPoint(4); // 1行目の'e'

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(9, frame.getActiveWindow().getPoint()); // 2行目の'h'の後ろ
    }

    @Test
    void 最終行では移動しない() {
        frame.getActiveWindow().insert("abc\ndef");
        frame.getActiveWindow().setPoint(5); // 2行目の'e'

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(5, frame.getActiveWindow().getPoint());
    }

    @Test
    void 行頭から次の行の行頭に移動する() {
        frame.getActiveWindow().insert("abc\ndef");
        frame.getActiveWindow().setPoint(0);

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(4, frame.getActiveWindow().getPoint()); // 2行目の行頭
    }

    @Test
    void 日本語テキストで次の行に移動する() {
        frame.getActiveWindow().insert("あいう\nかきく");
        frame.getActiveWindow().setPoint(1); // 'い'

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(5, frame.getActiveWindow().getPoint()); // 'き'
    }
}
