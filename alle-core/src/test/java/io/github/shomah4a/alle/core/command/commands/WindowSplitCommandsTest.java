package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowTree;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WindowSplitCommandsTest {

    private Frame createFrame() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
        var window = new Window(buffer);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        return new Frame(window, minibuffer);
    }

    @Nested
    class SplitWindowBelow {

        @Test
        void 上下分割後にアクティブウィンドウが元のウィンドウに留まる() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            var context = TestCommandContextFactory.create(frame, new BufferManager());

            new SplitWindowBelowCommand().execute(context).join();

            assertSame(originalWindow, frame.getActiveWindow());
        }

        @Test
        void 上下分割後にウィンドウが2つになる() {
            var frame = createFrame();
            var context = TestCommandContextFactory.create(frame, new BufferManager());

            new SplitWindowBelowCommand().execute(context).join();

            assertEquals(2, frame.getWindowTree().windows().size());
        }

        @Test
        void 分割後の新ウィンドウが同一バッファを表示する() {
            var frame = createFrame();
            var originalBuffer = frame.getActiveWindow().getBuffer();
            var context = TestCommandContextFactory.create(frame, new BufferManager());

            new SplitWindowBelowCommand().execute(context).join();

            var windows = frame.getWindowTree().windows();
            assertEquals(originalBuffer, windows.get(0).getBuffer());
            assertEquals(originalBuffer, windows.get(1).getBuffer());
        }
    }

    @Nested
    class SplitWindowRight {

        @Test
        void 左右分割後にアクティブウィンドウが元のウィンドウに留まる() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            var context = TestCommandContextFactory.create(frame, new BufferManager());

            new SplitWindowRightCommand().execute(context).join();

            assertSame(originalWindow, frame.getActiveWindow());
        }

        @Test
        void 左右分割後にウィンドウが2つになる() {
            var frame = createFrame();
            var context = TestCommandContextFactory.create(frame, new BufferManager());

            new SplitWindowRightCommand().execute(context).join();

            assertEquals(2, frame.getWindowTree().windows().size());
        }
    }

    @Nested
    class DeleteWindow {

        @Test
        void 分割後にアクティブウィンドウを閉じると1つに戻る() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var context = TestCommandContextFactory.create(frame, bufferManager);
            // まず分割
            new SplitWindowBelowCommand().execute(context).join();
            assertEquals(2, frame.getWindowTree().windows().size());

            // C-x oで新ウィンドウに移動してから削除
            frame.nextWindow();
            var deleteContext = TestCommandContextFactory.create(frame, bufferManager);
            new DeleteWindowCommand().execute(deleteContext).join();

            assertEquals(1, frame.getWindowTree().windows().size());
        }

        @Test
        void ウィンドウが1つしかない場合は何も起きない() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            var context = TestCommandContextFactory.create(frame, new BufferManager());

            new DeleteWindowCommand().execute(context).join();

            assertSame(originalWindow, frame.getActiveWindow());
            assertTrue(frame.getWindowTree() instanceof WindowTree.Leaf);
        }
    }

    @Nested
    class DeleteOtherWindows {

        @Test
        void 複数ウィンドウからアクティブウィンドウのみが残る() {
            var frame = createFrame();
            var originalWindow = frame.getActiveWindow();
            var bufferManager = new BufferManager();
            var context = TestCommandContextFactory.create(frame, bufferManager);

            // 2回分割して3ウィンドウにする
            new SplitWindowBelowCommand().execute(context).join();
            var context2 = TestCommandContextFactory.create(frame, bufferManager);
            new SplitWindowBelowCommand().execute(context2).join();
            assertEquals(3, frame.getWindowTree().windows().size());

            // delete-other-windowsで元のウィンドウのみ残す
            var context3 = TestCommandContextFactory.create(frame, bufferManager);
            new DeleteOtherWindowsCommand().execute(context3).join();

            assertEquals(1, frame.getWindowTree().windows().size());
            assertSame(originalWindow, frame.getActiveWindow());
        }

        @Test
        void ウィンドウが1つの場合でもエラーにならない() {
            var frame = createFrame();
            var context = TestCommandContextFactory.create(frame, new BufferManager());

            new DeleteOtherWindowsCommand().execute(context).join();

            assertEquals(1, frame.getWindowTree().windows().size());
        }
    }
}
