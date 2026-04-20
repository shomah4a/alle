package io.github.shomah4a.alle.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.command.OverridingKeymapController;
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
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QueryReplaceCommandTest {

    private static class CapturingOverridingKeymapController implements OverridingKeymapController {
        @Nullable
        Keymap currentKeymap;

        @Override
        public void set(Keymap keymap, Runnable onUnboundKeyExit) {
            this.currentKeymap = keymap;
        }

        @Override
        public void clear() {
            this.currentKeymap = null;
        }
    }

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

    private record Setup(
            CommandContext context,
            BufferFacade buffer,
            Window window,
            CapturingOverridingKeymapController controller,
            MessageBuffer messageBuffer) {}

    private static Setup setup(String text, String... responses) {
        var settings = new SettingsRegistry();
        var buffer = new BufferFacade(new TextBuffer("target", new GapTextModel(), settings));
        if (!text.isEmpty()) {
            buffer.insertText(0, text);
        }
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settings)));
        var frame = new Frame(window, minibuffer);

        var bufferManager = new BufferManager();
        bufferManager.add(buffer);

        var controller = new CapturingOverridingKeymapController();
        var messageBuffer = new MessageBuffer("*Messages*", 100, settings);
        var warningBuffer = new MessageBuffer("*Warnings*", 100, settings);
        var registry = new CommandRegistry();

        var context = new CommandContext(
                frame,
                bufferManager,
                window,
                queuePrompter(responses),
                Lists.immutable.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                messageBuffer,
                warningBuffer,
                settings,
                new CommandResolver(registry),
                controller);

        return new Setup(context, buffer, window, controller, messageBuffer);
    }

    @Nested
    class QueryReplace {

        @Test
        void FROMがキャンセルされた場合は何もせず終了する() {
            var s = setup("foo bar");
            var cmd = new QueryReplaceCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNull(s.controller.currentKeymap);
            assertEquals("foo bar", s.buffer.getText());
        }

        @Test
        void FROMが空文字の場合は何もせず終了する() {
            var s = setup("foo bar", "", "ignored");
            var cmd = new QueryReplaceCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNull(s.controller.currentKeymap);
            assertEquals("foo bar", s.buffer.getText());
        }

        @Test
        void TOがキャンセルされた場合は置換せず終了する() {
            // FROMは "foo"、TOプロンプトでキャンセル（=Cancelledが返る）
            var s = setup("foo bar", "foo");
            var cmd = new QueryReplaceCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNull(s.controller.currentKeymap);
            assertEquals("foo bar", s.buffer.getText());
        }

        @Test
        void マッチがある場合にセッションが開始される() {
            var s = setup("foo bar foo", "foo", "baz");
            var cmd = new QueryReplaceCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNotNull(s.controller.currentKeymap);
            assertEquals("query-replace", s.controller.currentKeymap.getName());
        }

        @Test
        void マッチがない場合はセッションが開始されずReplaced0が表示される() {
            var s = setup("hello world", "xyz", "abc");
            var cmd = new QueryReplaceCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNull(s.controller.currentKeymap);
            assertTrue(s.messageBuffer.getLastMessage().orElse("").startsWith("Replaced 0"));
        }

        @Test
        void point以降のみ置換範囲になる() {
            var s = setup("foo bar foo", "foo", "X");
            s.window.setPoint(4); // " bar foo" の " " 位置
            var cmd = new QueryReplaceCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNotNull(s.controller.currentKeymap);
            // 最初のマッチは 8..11（point 4 以降の foo）
            // セッションが起動していることを確認
            assertFalse(s.buffer.getText().startsWith("X"));
        }
    }

    @Nested
    class エイリアス {

        @Test
        void replace_stringという名前でも起動できる() {
            var s = setup("foo bar foo", "foo", "X");
            var cmd = new QueryReplaceCommand("replace-string", new InputHistory(), new InputHistory());

            assertEquals("replace-string", cmd.name());

            cmd.execute(s.context).join();

            assertNotNull(s.controller.currentKeymap);
        }

        @Test
        void replace_regexpという名前でも起動できる() {
            var s = setup("abc 123", "\\d+", "N");
            var cmd = new QueryReplaceRegexpCommand("replace-regexp", new InputHistory(), new InputHistory());

            assertEquals("replace-regexp", cmd.name());

            cmd.execute(s.context).join();

            assertNotNull(s.controller.currentKeymap);
        }
    }

    @Nested
    class QueryReplaceRegexp {

        @Test
        void 正規表現でセッションが開始される() {
            var s = setup("abc 123 def 456", "\\d+", "N");
            var cmd = new QueryReplaceRegexpCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNotNull(s.controller.currentKeymap);
        }

        @Test
        void 無効な正規表現はエラーで終了する() {
            var s = setup("abc", "[unclosed", "ignored");
            var cmd = new QueryReplaceRegexpCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNull(s.controller.currentKeymap);
            assertTrue(s.messageBuffer.getLastMessage().orElse("").startsWith("Invalid regexp"));
        }

        @Test
        void 正規表現マッチがないときはReplaced0で終了する() {
            var s = setup("abc def", "\\d+", "N");
            var cmd = new QueryReplaceRegexpCommand(new InputHistory(), new InputHistory());

            cmd.execute(s.context).join();

            assertNull(s.controller.currentKeymap);
            assertTrue(s.messageBuffer.getLastMessage().orElse("").startsWith("Replaced 0"));
        }
    }
}
