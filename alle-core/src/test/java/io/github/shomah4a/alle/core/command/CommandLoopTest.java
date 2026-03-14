package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.input.InputSource;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Iterator;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommandLoopTest {

    private static InputSource fromKeyStrokes(ImmutableList<KeyStroke> keyStrokes) {
        Iterator<KeyStroke> iterator = keyStrokes.iterator();
        return () -> iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    private Frame createFrame() {
        var buffer = new Buffer("test", new GapTextModel());
        var window = new Window(buffer);
        var minibuffer = new Window(new Buffer("*Minibuffer*", new GapTextModel()));
        return new Frame(window, minibuffer);
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

            var loop = new CommandLoop(input, resolver, frame, bufferManager);
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

            var loop = new CommandLoop(input, resolver, frame, bufferManager);
            loop.run();

            assertEquals("\u3042\u3044", frame.getActiveWindow().getBuffer().getText());
        }

        @Test
        void キーマップ未バインドの印字可能文字は挿入されない() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var resolver = new KeyResolver();
            var input = fromKeyStrokes(Lists.immutable.of(KeyStroke.of('H'), KeyStroke.of('i')));

            var loop = new CommandLoop(input, resolver, frame, bufferManager);
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

            var loop = new CommandLoop(input, resolver, frame, bufferManager);
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

            var loop = new CommandLoop(input, resolver, frame, bufferManager);
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

            var loop = new CommandLoop(input, resolver, frame, bufferManager);
            loop.run();

            assertEquals("", frame.getActiveWindow().getBuffer().getText());
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

            var loop = new CommandLoop(() -> Optional.empty(), resolver, frame, bufferManager);
            loop.processKey(KeyStroke.of('A'));

            assertEquals("A", frame.getActiveWindow().getBuffer().getText());
        }

        @Test
        void 未バインドのキーは無視される() {
            var frame = createFrame();
            var bufferManager = new BufferManager();
            var resolver = new KeyResolver();

            var loop = new CommandLoop(() -> Optional.empty(), resolver, frame, bufferManager);
            loop.processKey(KeyStroke.of('A'));

            assertEquals("", frame.getActiveWindow().getBuffer().getText());
        }
    }
}
