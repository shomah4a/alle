package io.github.shomah4a.alle.core.keybind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class KeymapTest {

    private Command dummyCommand(String name) {
        return new Command() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public CompletableFuture<Void> execute(CommandContext context) {
                return CompletableFuture.completedFuture(null);
            }
        };
    }

    @Nested
    class コマンドバインド {

        @Test
        void キーストロークにコマンドをバインドして検索できる() {
            var keymap = new Keymap("global");
            var cmd = dummyCommand("forward-char");
            keymap.bind(KeyStroke.ctrl('f'), cmd);

            var entry = keymap.lookup(KeyStroke.ctrl('f'));

            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("forward-char", binding.command().name());
        }

        @Test
        void バインドされていないキーはemptyを返す() {
            var keymap = new Keymap("global");
            assertTrue(keymap.lookup(KeyStroke.ctrl('z')).isEmpty());
        }
    }

    @Nested
    class プレフィックスバインド {

        @Test
        void キーストロークに子Keymapをバインドできる() {
            var keymap = new Keymap("global");
            var ctrlXMap = new Keymap("C-x");
            ctrlXMap.bind(KeyStroke.ctrl('s'), dummyCommand("save-buffer"));
            keymap.bindPrefix(KeyStroke.ctrl('x'), ctrlXMap);

            var entry = keymap.lookup(KeyStroke.ctrl('x'));

            assertTrue(entry.isPresent());
            var prefix = assertInstanceOf(KeymapEntry.PrefixBinding.class, entry.get());
            assertEquals("C-x", prefix.keymap().getName());

            var saveEntry = prefix.keymap().lookup(KeyStroke.ctrl('s'));
            assertTrue(saveEntry.isPresent());
            var saveBinding = assertInstanceOf(KeymapEntry.CommandBinding.class, saveEntry.get());
            assertEquals("save-buffer", saveBinding.command().name());
        }
    }

    @Nested
    class 印字可能文字の一括バインド {

        @Test
        void ASCII印字可能文字がバインドされる() {
            var keymap = new Keymap("global");
            var cmd = dummyCommand("self-insert-command");
            keymap.bindPrintableAscii(cmd);

            assertTrue(keymap.lookup(KeyStroke.of('a')).isPresent());
            assertTrue(keymap.lookup(KeyStroke.of('Z')).isPresent());
            assertTrue(keymap.lookup(KeyStroke.of(' ')).isPresent());
            assertTrue(keymap.lookup(KeyStroke.of('~')).isPresent());
        }

        @Test
        void 制御文字はバインドされない() {
            var keymap = new Keymap("global");
            var cmd = dummyCommand("self-insert-command");
            keymap.bindPrintableAscii(cmd);

            assertTrue(keymap.lookup(KeyStroke.of(0x1F)).isEmpty());
            assertTrue(keymap.lookup(KeyStroke.of(0x7F)).isEmpty());
        }
    }

    @Nested
    class デフォルトコマンド {

        @Test
        void 未バインドの印字可能文字にデフォルトコマンドが適用される() {
            var keymap = new Keymap("global");
            var cmd = dummyCommand("self-insert-command");
            keymap.setDefaultCommand(cmd);

            var entry = keymap.lookup(KeyStroke.of('a'));
            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("self-insert-command", binding.command().name());
        }

        @Test
        void 日本語文字にデフォルトコマンドが適用される() {
            var keymap = new Keymap("global");
            var cmd = dummyCommand("self-insert-command");
            keymap.setDefaultCommand(cmd);

            assertTrue(keymap.lookup(KeyStroke.of('\u3042')).isPresent());
        }

        @Test
        void 修飾キー付きにはデフォルトコマンドは適用されない() {
            var keymap = new Keymap("global");
            var cmd = dummyCommand("self-insert-command");
            keymap.setDefaultCommand(cmd);

            assertTrue(keymap.lookup(KeyStroke.ctrl('a')).isEmpty());
        }

        @Test
        void 制御文字にはデフォルトコマンドは適用されない() {
            var keymap = new Keymap("global");
            var cmd = dummyCommand("self-insert-command");
            keymap.setDefaultCommand(cmd);

            assertTrue(keymap.lookup(KeyStroke.of(0x01)).isEmpty());
        }

        @Test
        void 明示的バインドがデフォルトコマンドより優先される() {
            var keymap = new Keymap("global");
            var defaultCmd = dummyCommand("self-insert-command");
            var specificCmd = dummyCommand("specific-command");
            keymap.setDefaultCommand(defaultCmd);
            keymap.bind(KeyStroke.of('a'), specificCmd);

            var entry = keymap.lookup(KeyStroke.of('a'));
            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("specific-command", binding.command().name());
        }
    }

    @Nested
    class quitコマンド {

        @Test
        void Cgにquitコマンドがフォールバックで返される() {
            var quit = dummyCommand("keyboard-quit");
            Keymap.setQuitCommand(quit);
            var keymap = new Keymap("global");

            var entry = keymap.lookup(KeyStroke.ctrl('g'));
            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("keyboard-quit", binding.command().name());
        }

        @Test
        void 明示バインドがquitコマンドより優先される() {
            var quit = dummyCommand("keyboard-quit");
            var cancel = dummyCommand("minibuffer-cancel");
            Keymap.setQuitCommand(quit);
            var keymap = new Keymap("test");
            keymap.bind(KeyStroke.ctrl('g'), cancel);

            var entry = keymap.lookup(KeyStroke.ctrl('g'));
            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("minibuffer-cancel", binding.command().name());
        }

        @Test
        void プレフィックスキーマップでもquitコマンドが効く() {
            var quit = dummyCommand("keyboard-quit");
            Keymap.setQuitCommand(quit);
            var prefixKeymap = new Keymap("C-x");
            prefixKeymap.bind(KeyStroke.ctrl('s'), dummyCommand("save-buffer"));

            var entry = prefixKeymap.lookup(KeyStroke.ctrl('g'));
            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("keyboard-quit", binding.command().name());
        }
    }

    @Nested
    class 名前 {

        @Test
        void Keymap名を取得できる() {
            var keymap = new Keymap("global");
            assertEquals("global", keymap.getName());
        }
    }

    @Nested
    class isPrintable {

        @Test
        void ASCII印字可能文字はprintableである() {
            assertTrue(Keymap.isPrintable(KeyStroke.of('a')));
            assertTrue(Keymap.isPrintable(KeyStroke.of('Z')));
            assertTrue(Keymap.isPrintable(KeyStroke.of(' ')));
            assertTrue(Keymap.isPrintable(KeyStroke.of('~')));
        }

        @Test
        void 日本語文字はprintableである() {
            assertTrue(Keymap.isPrintable(KeyStroke.of('\u3042')));
        }

        @Test
        void 修飾キー付きはprintableでない() {
            assertFalse(Keymap.isPrintable(KeyStroke.ctrl('a')));
            assertFalse(Keymap.isPrintable(KeyStroke.meta('a')));
        }

        @Test
        void 制御文字はprintableでない() {
            assertFalse(Keymap.isPrintable(KeyStroke.of(0x01)));
            assertFalse(Keymap.isPrintable(KeyStroke.of(0x7F)));
        }

        static Stream<Arguments> privateUseAreaのコードポイント() {
            return Stream.of(
                    Arguments.of("ARROW_UP", KeyStroke.ARROW_UP),
                    Arguments.of("ARROW_DOWN", KeyStroke.ARROW_DOWN),
                    Arguments.of("ARROW_LEFT", KeyStroke.ARROW_LEFT),
                    Arguments.of("ARROW_RIGHT", KeyStroke.ARROW_RIGHT),
                    Arguments.of("Private Use Area先頭(U+E000)", 0xE000),
                    Arguments.of("Private Use Area末尾(U+F8FF)", 0xF8FF));
        }

        @ParameterizedTest(name = "Private Use Area({0})はprintableでない")
        @MethodSource("privateUseAreaのコードポイント")
        void PrivateUseAreaのコードポイントはprintableでない(String label, int codePoint) {
            assertFalse(Keymap.isPrintable(KeyStroke.of(codePoint)));
        }
    }
}
