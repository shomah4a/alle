package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Direction;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OtherWindowCommandTest {

    private Window createWindow(String name) {
        return new Window(new BufferFacade(new EditableBuffer(name, new GapTextModel(), new SettingsRegistry())));
    }

    @Nested
    class ウィンドウ切り替え {

        @Test
        void 二分割で次のウィンドウに切り替わる() {
            var windowA = createWindow("a");
            var minibuffer = createWindow("*Minibuffer*");
            var frame = new Frame(windowA, minibuffer);
            var bufferManager = new BufferManager();

            var bufferB = new BufferFacade(new EditableBuffer("b", new GapTextModel(), new SettingsRegistry()));
            frame.splitActiveWindow(Direction.VERTICAL, bufferB);
            // splitActiveWindow後、activeWindowは新しいウィンドウ(B)になっている
            var windowB = frame.getActiveWindow();
            // AをアクティブにしてからOtherWindowを実行
            frame.setActiveWindow(windowA);

            var cmd = new OtherWindowCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager);
            cmd.execute(context).join();

            assertSame(windowB, frame.getActiveWindow());
        }

        @Test
        void 最後のウィンドウから最初のウィンドウに循環する() {
            var windowA = createWindow("a");
            var minibuffer = createWindow("*Minibuffer*");
            var frame = new Frame(windowA, minibuffer);
            var bufferManager = new BufferManager();

            var bufferB = new BufferFacade(new EditableBuffer("b", new GapTextModel(), new SettingsRegistry()));
            frame.splitActiveWindow(Direction.VERTICAL, bufferB);
            var windowB = frame.getActiveWindow();
            // windowBがアクティブの状態でOtherWindow実行 → windowAに戻る
            assertSame(windowB, frame.getActiveWindow());

            var cmd = new OtherWindowCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager);
            cmd.execute(context).join();

            assertSame(windowA, frame.getActiveWindow());
        }
    }

    @Nested
    class ウィンドウが1つの場合 {

        @Test
        void 何も変わらない() {
            var windowA = createWindow("a");
            var minibuffer = createWindow("*Minibuffer*");
            var frame = new Frame(windowA, minibuffer);
            var bufferManager = new BufferManager();

            var cmd = new OtherWindowCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager);
            cmd.execute(context).join();

            assertSame(windowA, frame.getActiveWindow());
        }
    }

    @Nested
    class ミニバッファアクティブ中 {

        @Test
        void ツリー内のウィンドウに移動する() {
            var windowA = createWindow("a");
            var minibuffer = createWindow("*Minibuffer*");
            var frame = new Frame(windowA, minibuffer);
            var bufferManager = new BufferManager();

            var bufferB = new BufferFacade(new EditableBuffer("b", new GapTextModel(), new SettingsRegistry()));
            frame.splitActiveWindow(Direction.VERTICAL, bufferB);
            frame.activateMinibuffer();
            assertSame(minibuffer, frame.getActiveWindow());

            var cmd = new OtherWindowCommand();
            var context = TestCommandContextFactory.create(frame, bufferManager);
            cmd.execute(context).join();

            assertSame(windowA, frame.getActiveWindow());
        }
    }
}
