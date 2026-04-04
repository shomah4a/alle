package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Direction;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.FrameLayoutStore;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowTree;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RestoreFrameStateCommandTest {

    private final SettingsRegistry settings = new SettingsRegistry();
    private Frame frame;
    private BufferManager bufferManager;
    private FrameLayoutStore layoutStore;
    private BufferFacade scratchBuffer;

    @BeforeEach
    void setUp() {
        scratchBuffer = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), settings));
        var window = new Window(scratchBuffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settings)));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(scratchBuffer);
        layoutStore = new FrameLayoutStore();
    }

    private InputPrompter confirming(String value) {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private InputPrompter cancelling() {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
    }

    private BufferFacade createBuffer(String name) {
        var buf = new BufferFacade(new TextBuffer(name, new GapTextModel(), settings));
        bufferManager.add(buf);
        return buf;
    }

    @Nested
    class 復元 {

        @Test
        void 保存済みレイアウトを復元する() {
            var buf1 = createBuffer("a.txt");
            var buf2 = createBuffer("b.txt");
            frame.getActiveWindow().setBuffer(buf1);
            frame.splitActiveWindow(Direction.VERTICAL, buf2);

            var saveCmd = new SaveFrameStateCommand(layoutStore, new InputHistory());
            var saveCtx = TestCommandContextFactory.create(frame, bufferManager, confirming("split-layout"));
            saveCmd.execute(saveCtx).join();

            frame.deleteOtherWindows();
            frame.getActiveWindow().setBuffer(scratchBuffer);

            var restoreCmd = new RestoreFrameStateCommand(layoutStore, new InputHistory(), () -> scratchBuffer);
            var restoreCtx = TestCommandContextFactory.create(frame, bufferManager, confirming("split-layout"));
            restoreCmd.execute(restoreCtx).join();

            assertInstanceOf(WindowTree.Split.class, frame.getWindowTree());
            assertEquals(2, frame.getWindowTree().windows().size());
        }

        @Test
        void 存在しないレイアウト名の場合メッセージを表示する() {
            var restoreCmd = new RestoreFrameStateCommand(layoutStore, new InputHistory(), () -> scratchBuffer);
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("nonexistent"));

            restoreCmd.execute(context).join();

            assertEquals(
                    "Frame state not found: nonexistent",
                    context.messageBuffer().getLastMessage().orElse(""));
        }

        @Test
        void 空名の場合復元しない() {
            var restoreCmd = new RestoreFrameStateCommand(layoutStore, new InputHistory(), () -> scratchBuffer);
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming(""));

            restoreCmd.execute(context).join();

            assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
        }

        @Test
        void キャンセル時は何も変わらない() {
            var restoreCmd = new RestoreFrameStateCommand(layoutStore, new InputHistory(), () -> scratchBuffer);
            var context = TestCommandContextFactory.create(frame, bufferManager, cancelling());

            restoreCmd.execute(context).join();

            assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
        }
    }

    @Test
    void コマンド名はrestoreMinusframeMinusstateである() {
        var cmd = new RestoreFrameStateCommand(layoutStore, new InputHistory(), () -> scratchBuffer);
        assertEquals("restore-frame-state", cmd.name());
    }
}
