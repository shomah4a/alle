package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PreviousLineCommandTest {

    private Frame frame;
    private BufferManager bufferManager;
    private PreviousLineCommand cmd;

    @BeforeEach
    void setUp() {
        var buffer = new EditableBuffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
        cmd = new PreviousLineCommand();
    }

    @Test
    void 前の行の同じカラム位置に移動する() {
        frame.getActiveWindow().insert("abc\ndef\nghi");
        frame.getActiveWindow().setPoint(5); // 2行目の'e'

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(1, frame.getActiveWindow().getPoint()); // 1行目の'b'
    }

    @Test
    void 前の行が短い場合は行末に移動する() {
        frame.getActiveWindow().insert("ab\ncdefgh\nijklmn");
        frame.getActiveWindow().setPoint(7); // 2行目の'f'

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(2, frame.getActiveWindow().getPoint()); // 1行目の'b'の後ろ
    }

    @Test
    void 先頭行では移動しない() {
        frame.getActiveWindow().insert("abc\ndef");
        frame.getActiveWindow().setPoint(1); // 1行目の'b'

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(1, frame.getActiveWindow().getPoint());
    }

    @Test
    void 行頭から前の行の行頭に移動する() {
        frame.getActiveWindow().insert("abc\ndef");
        frame.getActiveWindow().setPoint(4); // 2行目の行頭

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(0, frame.getActiveWindow().getPoint()); // 1行目の行頭
    }

    @Test
    void 日本語テキストで前の行に移動する() {
        frame.getActiveWindow().insert("あいう\nかきく");
        frame.getActiveWindow().setPoint(5); // 'き'

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(1, frame.getActiveWindow().getPoint()); // 'い'
    }
}
