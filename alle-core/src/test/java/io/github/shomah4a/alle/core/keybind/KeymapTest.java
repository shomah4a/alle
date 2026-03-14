package io.github.shomah4a.alle.core.keybind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KeymapTest {

    private Command dummyCommand(String name) {
        return new Command() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void execute(CommandContext context) {}
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
    class 名前 {

        @Test
        void Keymap名を取得できる() {
            var keymap = new Keymap("global");
            assertEquals("global", keymap.getName());
        }
    }
}
