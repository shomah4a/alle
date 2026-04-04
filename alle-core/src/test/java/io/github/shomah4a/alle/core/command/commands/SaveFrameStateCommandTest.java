package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.TestCommandContextFactory;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.BufferIdentifier;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.FrameLayoutStore;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowTreeSnapshot;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SaveFrameStateCommandTest {

    private Frame frame;
    private BufferManager bufferManager;
    private FrameLayoutStore layoutStore;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("test.txt", new GapTextModel(), new SettingsRegistry()));
        buffer.insertText(0, "hello");
        var window = new Window(buffer);
        var minibuffer = new Window(
                new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
        layoutStore = new FrameLayoutStore();
    }

    private InputPrompter confirming(String value) {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
    }

    private InputPrompter cancelling() {
        return (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
    }

    @Nested
    class 保存 {

        @Test
        void 名前を指定してフレーム状態を保存する() {
            var cmd = new SaveFrameStateCommand(layoutStore, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming("my-layout"));

            cmd.execute(context).join();

            assertTrue(layoutStore.load("my-layout").isPresent());
            var snapshot = layoutStore.load("my-layout").orElseThrow();
            var leaf = (WindowTreeSnapshot.Leaf) snapshot.tree();
            assertEquals(
                    new BufferIdentifier.ByName("test.txt"),
                    leaf.snapshot().current().identifier());
        }

        @Test
        void 空名の場合保存せずメッセージを表示する() {
            var cmd = new SaveFrameStateCommand(layoutStore, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, confirming(""));

            cmd.execute(context).join();

            assertTrue(layoutStore.names().isEmpty());
        }

        @Test
        void キャンセル時は何も保存しない() {
            var cmd = new SaveFrameStateCommand(layoutStore, new InputHistory());
            var context = TestCommandContextFactory.create(frame, bufferManager, cancelling());

            cmd.execute(context).join();

            assertTrue(layoutStore.names().isEmpty());
        }
    }

    @Test
    void コマンド名はsaveMinusframeMinusstateである() {
        var cmd = new SaveFrameStateCommand(layoutStore, new InputHistory());
        assertEquals("save-frame-state", cmd.name());
    }
}
