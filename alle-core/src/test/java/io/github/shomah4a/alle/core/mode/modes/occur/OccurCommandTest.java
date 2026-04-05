package io.github.shomah4a.alle.core.mode.modes.occur;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.command.NoOpOverridingKeymapController;
import io.github.shomah4a.alle.core.input.Completer;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OccurCommandTest {

    private static InputPrompter queuePrompter(String... responses) {
        var queue = new LinkedBlockingQueue<String>();
        for (String r : responses) {
            queue.add(r);
        }
        return new InputPrompter() {
            @Override
            public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
                String value = queue.poll();
                if (value == null) {
                    return CompletableFuture.completedFuture(new PromptResult.Cancelled());
                }
                return CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
            }

            @Override
            public CompletableFuture<PromptResult> prompt(
                    String message, String initialValue, InputHistory history, Completer completer) {
                return prompt(message, history);
            }
        };
    }

    private record OccurSetup(CommandContext context, OccurCommand command, Frame frame, BufferManager bufferManager) {}

    private static OccurSetup setup(String sourceText, String... promptResponses) {
        var settings = new SettingsRegistry();
        var sourceBuffer = new BufferFacade(new TextBuffer("source.txt", new GapTextModel(), settings));
        if (!sourceText.isEmpty()) {
            sourceBuffer.insertText(0, sourceText);
        }

        var window = new Window(sourceBuffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settings)));
        var frame = new Frame(window, minibuffer);

        var bufferManager = new BufferManager();
        bufferManager.add(sourceBuffer);

        var prompter = queuePrompter(promptResponses);
        var registry = new CommandRegistry();
        var context = new CommandContext(
                frame,
                bufferManager,
                window,
                prompter,
                Lists.immutable.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, settings),
                new MessageBuffer("*Warnings*", 100, settings),
                settings,
                new CommandResolver(registry),
                new NoOpOverridingKeymapController());

        var occurCommand =
                new OccurCommand(new InputHistory(), new Keymap("occur-test"), new CommandRegistry(), settings);

        return new OccurSetup(context, occurCommand, frame, bufferManager);
    }

    @Nested
    class execute {

        @Test
        void occurバッファが作成されウィンドウが分割される() {
            var s = setup("foo\nbar\nfoo bar", "foo");
            s.command.execute(s.context).join();

            assertTrue(s.bufferManager.findByName("*Occur source.txt*").isPresent());
            assertEquals(2, s.frame.getWindowTree().windows().size());
        }

        @Test
        void occurバッファがread_onlyに設定される() {
            var s = setup("foo\nbar\nfoo bar", "foo");
            s.command.execute(s.context).join();

            var occurBuffer = s.bufferManager.findByName("*Occur source.txt*").orElseThrow();
            assertTrue(occurBuffer.isReadOnly());
        }

        @Test
        void occurバッファがdirtyでない() {
            var s = setup("foo\nbar\nfoo bar", "foo");
            s.command.execute(s.context).join();

            var occurBuffer = s.bufferManager.findByName("*Occur source.txt*").orElseThrow();
            assertFalse(occurBuffer.isDirty());
        }

        @Test
        void occurバッファにマッチ結果が表示される() {
            var s = setup("foo\nbar\nfoo bar", "foo");
            s.command.execute(s.context).join();

            var occurBuffer = s.bufferManager.findByName("*Occur source.txt*").orElseThrow();
            String text = occurBuffer.getText();
            assertTrue(text.startsWith("2 lines matching \"foo\" in source.txt"));
        }

        @Test
        void フォーカスがoccurバッファに移る() {
            var s = setup("foo\nbar\nfoo bar", "foo");
            s.command.execute(s.context).join();

            var activeBuffer = s.frame.getActiveWindow().getBuffer();
            assertEquals("*Occur source.txt*", activeBuffer.getName());
        }

        @Test
        void 同じバッファで再度occurすると内容が上書きされる() {
            var s = setup("foo\nbar\nfoo bar\nbaz foo", "foo", "bar");

            // 1回目: "foo" で検索
            s.command.execute(s.context).join();
            var occurBuffer = s.bufferManager.findByName("*Occur source.txt*").orElseThrow();
            assertTrue(occurBuffer.getText().contains("\"foo\""));

            // 2回目のcontextを作成（アクティブウィンドウをソースバッファに戻す）
            var sourceBuffer = s.bufferManager.findByName("source.txt").orElseThrow();
            var sourceWindow =
                    s.frame.getWindowTree().windows().detect(w -> w.getBuffer().equals(sourceBuffer));
            s.frame.setActiveWindow(sourceWindow);

            var context2 = new CommandContext(
                    s.context.frame(),
                    s.context.bufferManager(),
                    sourceWindow,
                    s.context.inputPrompter(),
                    Lists.immutable.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    s.context.killRing(),
                    s.context.messageBuffer(),
                    s.context.warningBuffer(),
                    s.context.settingsRegistry(),
                    s.context.commandResolver(),
                    s.context.overridingKeymapController());

            // 2回目: "bar" で検索
            s.command.execute(context2).join();
            var occurBuffer2 = s.bufferManager.findByName("*Occur source.txt*").orElseThrow();
            assertTrue(occurBuffer2.getText().contains("\"bar\""));
            assertTrue(occurBuffer2.isReadOnly());
            assertFalse(occurBuffer2.isDirty());
        }

        @Test
        void クエリが空の場合はoccurバッファが作成されない() {
            var s = setup("foo\nbar", "");
            s.command.execute(s.context).join();

            assertTrue(s.bufferManager.findByName("*Occur source.txt*").isEmpty());
            assertEquals(1, s.frame.getWindowTree().windows().size());
        }

        @Test
        void キャンセルした場合はoccurバッファが作成されない() {
            var s = setup("foo\nbar");
            s.command.execute(s.context).join();

            assertTrue(s.bufferManager.findByName("*Occur source.txt*").isEmpty());
            assertEquals(1, s.frame.getWindowTree().windows().size());
        }
    }
}
