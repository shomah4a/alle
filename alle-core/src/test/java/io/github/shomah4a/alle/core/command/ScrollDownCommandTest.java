package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.ViewportSize;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScrollDownCommandTest {

    private Frame frame;
    private BufferManager bufferManager;
    private ScrollDownCommand cmd;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
        cmd = new ScrollDownCommand();
    }

    @Test
    void 表示行数分だけ上にスクロールしオーバーラップ2行を残す() {
        var sb = new StringBuilder();
        for (int i = 0; i < 19; i++) {
            sb.append("line").append(i).append("\n");
        }
        sb.append("line19");
        frame.getActiveWindow().insert(sb.toString());
        // 19行目の行頭にカーソルを置く
        int line19Start = frame.getActiveWindow().getBuffer().lineStartOffset(19);
        frame.getActiveWindow().setPoint(line19Start);
        frame.getActiveWindow().setDisplayStartLine(10);
        frame.getActiveWindow().setViewportSize(new ViewportSize(10, 80));

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(2, frame.getActiveWindow().getDisplayStartLine());
        // カラム0を維持して11行目の行頭に移動
        int expectedPoint = frame.getActiveWindow().getBuffer().lineStartOffset(11);
        assertEquals(expectedPoint, frame.getActiveWindow().getPoint());
    }

    @Test
    void スクロール後にカラム位置を維持する() {
        var sb = new StringBuilder();
        for (int i = 0; i < 19; i++) {
            sb.append("abcdefgh").append(i).append("\n");
        }
        sb.append("abcdefgh19");
        frame.getActiveWindow().insert(sb.toString());
        // 15行目のカラム5にカーソルを置く
        int line15Start = frame.getActiveWindow().getBuffer().lineStartOffset(15);
        frame.getActiveWindow().setPoint(line15Start + 5);
        frame.getActiveWindow().setDisplayStartLine(10);
        frame.getActiveWindow().setViewportSize(new ViewportSize(10, 80));

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        // 7行目のカラム5に移動
        int expectedPoint = frame.getActiveWindow().getBuffer().lineStartOffset(7) + 5;
        assertEquals(expectedPoint, frame.getActiveWindow().getPoint());
    }

    @Test
    void 先頭付近ではdisplayStartLineが0未満にならない() {
        var sb = new StringBuilder();
        for (int i = 0; i < 19; i++) {
            sb.append("line").append(i).append("\n");
        }
        sb.append("line19");
        frame.getActiveWindow().insert(sb.toString());
        // 5行目の行頭にカーソルを置く
        int line5Start = frame.getActiveWindow().getBuffer().lineStartOffset(5);
        frame.getActiveWindow().setPoint(line5Start);
        frame.getActiveWindow().setDisplayStartLine(3);
        frame.getActiveWindow().setViewportSize(new ViewportSize(10, 80));

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(0, frame.getActiveWindow().getDisplayStartLine());
        // 5 - 8 = -3 → 0行目にクランプ
        assertEquals(0, frame.getActiveWindow().getPoint());
    }

    @Test
    void viewportSizeが未設定の場合は何もしない() {
        frame.getActiveWindow().insert("abc\ndef\nghi");
        frame.getActiveWindow().setPoint(4);
        frame.getActiveWindow().setDisplayStartLine(1);

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(4, frame.getActiveWindow().getPoint());
        assertEquals(1, frame.getActiveWindow().getDisplayStartLine());
    }
}
