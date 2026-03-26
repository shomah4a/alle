package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommandLoopTest {

    private static final InputPrompter NOOP_PROMPTER =
            (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());

    private static InputSource fromKeyStrokes(ImmutableList<KeyStroke> keyStrokes) {
        Iterator<KeyStroke> iterator = keyStrokes.iterator();
        return () -> iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    private static final SettingsRegistry SETTINGS = new SettingsRegistry();

    private Frame createFrame() {
        var buffer = new BufferFacade(new TextBuffer("test", new GapTextModel(), SETTINGS));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
        return new Frame(window, minibuffer);
    }

    private CommandLoop createLoop(InputSource input, KeyResolver resolver, Frame frame, BufferManager bufferManager) {
        return new CommandLoop(
                input,
                resolver,
                frame,
                bufferManager,
                NOOP_PROMPTER,
                new KillRing(),
                new MessageBuffer("*Messages*", 100, SETTINGS),
                new MessageBuffer("*Warnings*", 100, SETTINGS),
                SETTINGS);
    }

    @Nested
    class 印字可能文字の入力 {

        @Test
        void デフォルトコマンドにより印字可能文字がself_insertされる() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var keymap = new Keymap("global");
            keymap.setDefaultCommand(new SelfInsertCommand());
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);
            var input = fromKeyStrokes(Lists.immutable.of(KeyStroke.of('H'), KeyStroke.of('i')));

            var loop = createLoop(input, resolver, frame, bufferManager);
            loop.run();

            assertEquals("Hi", frame.getActiveWindow().getBuffer().getText());
        }

        @Test
        void デフォルトコマンドにより日本語文字がself_insertされる() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var keymap = new Keymap("global");
            keymap.setDefaultCommand(new SelfInsertCommand());
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);
            var input = fromKeyStrokes(Lists.immutable.of(KeyStroke.of('\u3042'), KeyStroke.of('\u3044')));

            var loop = createLoop(input, resolver, frame, bufferManager);
            loop.run();

            assertEquals("\u3042\u3044", frame.getActiveWindow().getBuffer().getText());
        }

        @Test
        void キーマップ未バインドの印字可能文字は挿入されない() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var resolver = new KeyResolver();
            var input = fromKeyStrokes(Lists.immutable.of(KeyStroke.of('H'), KeyStroke.of('i')));

            var loop = createLoop(input, resolver, frame, bufferManager);
            loop.run();

            assertEquals("", frame.getActiveWindow().getBuffer().getText());
        }
    }

    @Nested
    class キーバインドされたコマンド {

        @Test
        void バインドされたコマンドが実行される() {
            var frame = createFrame();
            frame.getActiveWindow().insert("Hello");
            frame.getActiveWindow().setPoint(0);
            var bufferManager = new BufferManager();

            var keymap = new Keymap("global");
            keymap.bind(KeyStroke.ctrl('f'), new ForwardCharCommand());
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);

            var input = fromKeyStrokes(Lists.immutable.of(KeyStroke.ctrl('f'), KeyStroke.ctrl('f')));

            var loop = createLoop(input, resolver, frame, bufferManager);
            loop.run();

            assertEquals(2, frame.getActiveWindow().getPoint());
        }
    }

    @Nested
    class プレフィックスキー {

        @Test
        void プレフィックスキー経由でコマンドが実行される() {
            var frame = createFrame();
            frame.getActiveWindow().insert("Hello");
            var bufferManager = new BufferManager();

            var ctrlXMap = new Keymap("C-x");
            ctrlXMap.bind(KeyStroke.ctrl('b'), new BackwardCharCommand());

            var keymap = new Keymap("global");
            keymap.bindPrefix(KeyStroke.ctrl('x'), ctrlXMap);
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);

            var input = fromKeyStrokes(Lists.immutable.of(KeyStroke.ctrl('x'), KeyStroke.ctrl('b')));

            var loop = createLoop(input, resolver, frame, bufferManager);
            loop.run();

            assertEquals(4, frame.getActiveWindow().getPoint());
        }

        @Test
        void プレフィックスキー後に未バインドのキーが来ても例外にならない() {
            var frame = createFrame();
            var bufferManager = new BufferManager();

            var ctrlXMap = new Keymap("C-x");
            var keymap = new Keymap("global");
            keymap.bindPrefix(KeyStroke.ctrl('x'), ctrlXMap);
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);

            var input = fromKeyStrokes(Lists.immutable.of(KeyStroke.ctrl('x'), KeyStroke.ctrl('z')));

            var loop = createLoop(input, resolver, frame, bufferManager);
            loop.run();

            assertEquals("", frame.getActiveWindow().getBuffer().getText());
        }
    }

    @Nested
    class コマンド履歴 {

        /**
         * 実行時のCommandContextをキャプチャするコマンド。
         */
        private static class CapturingCommand implements Command {

            private final String commandName;
            final MutableList<CommandContext> capturedContexts = Lists.mutable.empty();

            CapturingCommand(String commandName) {
                this.commandName = commandName;
            }

            @Override
            public String name() {
                return commandName;
            }

            @Override
            public CompletableFuture<Void> execute(CommandContext context) {
                capturedContexts.add(context);
                return CompletableFuture.completedFuture(null);
            }
        }

        @Test
        void 最初のコマンド実行時はlastCommandがemptyである() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var cmd = new CapturingCommand("test-cmd");
            var keymap = new Keymap("global");
            keymap.bind(KeyStroke.ctrl('a'), cmd);
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);

            var loop = createLoop(() -> Optional.empty(), resolver, frame, bufferManager);
            loop.processKey(KeyStroke.ctrl('a'));

            assertEquals(1, cmd.capturedContexts.size());
            assertTrue(cmd.capturedContexts.get(0).lastCommand().isEmpty());
            assertEquals("test-cmd", cmd.capturedContexts.get(0).thisCommand().orElseThrow());
        }

        @Test
        void 連続実行時にlastCommandが前回のコマンド名になる() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var cmd1 = new CapturingCommand("first-cmd");
            var cmd2 = new CapturingCommand("second-cmd");
            var keymap = new Keymap("global");
            keymap.bind(KeyStroke.ctrl('a'), cmd1);
            keymap.bind(KeyStroke.ctrl('b'), cmd2);
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);

            var loop = createLoop(() -> Optional.empty(), resolver, frame, bufferManager);
            loop.processKey(KeyStroke.ctrl('a'));
            loop.processKey(KeyStroke.ctrl('b'));

            assertEquals(1, cmd2.capturedContexts.size());
            assertEquals("first-cmd", cmd2.capturedContexts.get(0).lastCommand().orElseThrow());
            assertEquals(
                    "second-cmd", cmd2.capturedContexts.get(0).thisCommand().orElseThrow());
        }

        @Test
        void 同一コマンドの連続実行でlastCommandが自身の名前になる() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var cmd = new CapturingCommand("repeat-cmd");
            var keymap = new Keymap("global");
            keymap.bind(KeyStroke.ctrl('a'), cmd);
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);

            var loop = createLoop(() -> Optional.empty(), resolver, frame, bufferManager);
            loop.processKey(KeyStroke.ctrl('a'));
            loop.processKey(KeyStroke.ctrl('a'));

            assertEquals(2, cmd.capturedContexts.size());
            assertTrue(cmd.capturedContexts.get(0).lastCommand().isEmpty());
            assertEquals("repeat-cmd", cmd.capturedContexts.get(1).lastCommand().orElseThrow());
        }
    }

    @Nested
    class readOnly領域での編集 {

        @Test
        void readOnly領域でdeleteBackwardしても例外でスレッドが落ちない() {
            var frame = createFrame();
            var bufferFacade = frame.getActiveWindow().getBuffer();
            frame.getActiveWindow().insert("Find file: ");
            // プロンプト部分をread-onlyにする
            bufferFacade.putReadOnly(0, 11);
            // カーソルをread-only領域の末尾（プロンプト直後）に置く
            frame.getActiveWindow().setPoint(11);
            var bufferManager = new BufferManager();
            var messageBuffer = new MessageBuffer("*Messages*", 100, new SettingsRegistry());

            var keymap = new Keymap("global");
            keymap.bind(KeyStroke.of(0x7F), new BackwardDeleteCharCommand());
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);

            var loop = new CommandLoop(
                    () -> Optional.empty(),
                    resolver,
                    frame,
                    bufferManager,
                    NOOP_PROMPTER,
                    new KillRing(),
                    messageBuffer,
                    new MessageBuffer("*Warnings*", 100, new SettingsRegistry()),
                    new SettingsRegistry());
            // 例外でスレッドが落ちずにメッセージが表示されること
            loop.processKey(KeyStroke.of(0x7F));

            assertEquals("Find file: ", bufferFacade.getText());
            assertEquals("Text is read-only", messageBuffer.lineText(0));
        }
    }

    @Nested
    class processKey {

        @Test
        void バインド済みの単一キーを処理できる() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var keymap = new Keymap("global");
            keymap.setDefaultCommand(new SelfInsertCommand());
            var resolver = new KeyResolver();
            resolver.addKeymap(keymap);

            var loop = createLoop(() -> Optional.empty(), resolver, frame, bufferManager);
            loop.processKey(KeyStroke.of('A'));

            assertEquals("A", frame.getActiveWindow().getBuffer().getText());
        }

        @Test
        void 未バインドのキーは無視される() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var resolver = new KeyResolver();

            var loop = createLoop(() -> Optional.empty(), resolver, frame, bufferManager);
            loop.processKey(KeyStroke.of('A'));

            assertEquals("", frame.getActiveWindow().getBuffer().getText());
        }
    }
}
