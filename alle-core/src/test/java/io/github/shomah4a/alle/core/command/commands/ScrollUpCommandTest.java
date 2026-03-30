package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.ViewportSize;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScrollUpCommandTest {

    private Frame frame;
    private BufferManager bufferManager;
    private ScrollUpCommand cmd;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        window.setTruncateLines(true);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
        cmd = new ScrollUpCommand();
    }

    @Test
    void 表示行数分だけ下にスクロールしオーバーラップ2行を残す() {
        // 20行のテキストを作成
        var sb = new StringBuilder();
        for (int i = 0; i < 19; i++) {
            sb.append("line").append(i).append("\n");
        }
        sb.append("line19");
        frame.getActiveWindow().insert(sb.toString());
        frame.getActiveWindow().setPoint(0);
        frame.getActiveWindow().setDisplayStartLine(0);
        frame.getActiveWindow().setViewportSize(new ViewportSize(10, 80));

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(8, frame.getActiveWindow().getDisplayStartLine());
        // カラム0を維持して8行目の行頭に移動
        int expectedPoint = frame.getActiveWindow().getBuffer().lineStartOffset(8);
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
        // 0行目のカラム3にカーソルを置く
        frame.getActiveWindow().setPoint(3);
        frame.getActiveWindow().setDisplayStartLine(0);
        frame.getActiveWindow().setViewportSize(new ViewportSize(10, 80));

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        // 8行目のカラム3に移動
        int expectedPoint = frame.getActiveWindow().getBuffer().lineStartOffset(8) + 3;
        assertEquals(expectedPoint, frame.getActiveWindow().getPoint());
    }

    @Test
    void 移動先の行が短い場合はカラム位置を行末にクランプする() {
        frame.getActiveWindow().insert("abcdefghij\na\nb\nc\nd\ne\nf\ng\nh\ni\nj\nk");
        // 0行目のカラム8にカーソルを置く
        frame.getActiveWindow().setPoint(8);
        frame.getActiveWindow().setDisplayStartLine(0);
        frame.getActiveWindow().setViewportSize(new ViewportSize(5, 80));

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        // 3行目 "c" はカラム1が最大
        int line3Start = frame.getActiveWindow().getBuffer().lineStartOffset(3);
        assertEquals(line3Start + 1, frame.getActiveWindow().getPoint());
    }

    @Test
    void 末尾付近ではdisplayStartLineがlineCountを超えない() {
        var sb = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            sb.append("line").append(i).append("\n");
        }
        sb.append("line9");
        frame.getActiveWindow().insert(sb.toString());
        frame.getActiveWindow().setPoint(0);
        frame.getActiveWindow().setDisplayStartLine(5);
        frame.getActiveWindow().setViewportSize(new ViewportSize(10, 80));

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(9, frame.getActiveWindow().getDisplayStartLine());
    }

    @Test
    void viewportSizeが未設定の場合は何もしない() {
        frame.getActiveWindow().insert("abc\ndef\nghi");
        frame.getActiveWindow().setPoint(0);

        var context = TestCommandContextFactory.create(frame, bufferManager);
        cmd.execute(context).join();

        assertEquals(0, frame.getActiveWindow().getPoint());
        assertEquals(0, frame.getActiveWindow().getDisplayStartLine());
    }
}
