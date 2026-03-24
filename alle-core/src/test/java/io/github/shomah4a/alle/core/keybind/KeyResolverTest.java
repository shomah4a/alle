package io.github.shomah4a.alle.core.keybind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class KeyResolverTest {

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
    class 優先順位 {

        @Test
        void 先に追加されたKeymapが優先される() {
            var resolver = new KeyResolver();
            var highPriority = new Keymap("minor");
            highPriority.bind(KeyStroke.ctrl('f'), dummyCommand("minor-forward"));
            var lowPriority = new Keymap("global");
            lowPriority.bind(KeyStroke.ctrl('f'), dummyCommand("global-forward"));

            resolver.addKeymap(highPriority);
            resolver.addKeymap(lowPriority);

            var entry = resolver.resolve(KeyStroke.ctrl('f'));
            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("minor-forward", binding.command().name());
        }

        @Test
        void 優先Keymapにないキーは次のKeymapから解決される() {
            var resolver = new KeyResolver();
            var highPriority = new Keymap("minor");
            var lowPriority = new Keymap("global");
            lowPriority.bind(KeyStroke.ctrl('s'), dummyCommand("save-buffer"));

            resolver.addKeymap(highPriority);
            resolver.addKeymap(lowPriority);

            var entry = resolver.resolve(KeyStroke.ctrl('s'));
            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("save-buffer", binding.command().name());
        }

        @Test
        void どのKeymapにもないキーはemptyを返す() {
            var resolver = new KeyResolver();
            resolver.addKeymap(new Keymap("empty"));
            assertTrue(resolver.resolve(KeyStroke.ctrl('z')).isEmpty());
        }
    }

    @Nested
    class 空のリゾルバ {

        @Test
        void Keymapがない場合はemptyを返す() {
            var resolver = new KeyResolver();
            assertTrue(resolver.resolve(KeyStroke.of('a')).isEmpty());
        }
    }

    @Nested
    class バッファキーマップ込みの4階層解決 {

        @Test
        void バッファローカルキーマップが最優先で解決される() {
            var resolver = new KeyResolver();
            var globalKeymap = new Keymap("global");
            globalKeymap.bind(KeyStroke.ctrl('g'), dummyCommand("global-cmd"));
            resolver.addKeymap(globalKeymap);

            var localKeymap = new Keymap("local");
            localKeymap.bind(KeyStroke.ctrl('g'), dummyCommand("local-cmd"));

            var entry = resolver.resolveWithBuffer(
                    KeyStroke.ctrl('g'), Optional.of(localKeymap), Lists.immutable.empty(), Optional.empty());

            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("local-cmd", binding.command().name());
        }

        @Test
        void マイナーモードキーマップがメジャーモードより優先される() {
            var resolver = new KeyResolver();
            var globalKeymap = new Keymap("global");
            resolver.addKeymap(globalKeymap);

            var majorKeymap = new Keymap("major");
            majorKeymap.bind(KeyStroke.ctrl('f'), dummyCommand("major-cmd"));

            var minorKeymap = new Keymap("minor");
            minorKeymap.bind(KeyStroke.ctrl('f'), dummyCommand("minor-cmd"));

            var entry = resolver.resolveWithBuffer(
                    KeyStroke.ctrl('f'), Optional.empty(), Lists.immutable.of(minorKeymap), Optional.of(majorKeymap));

            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("minor-cmd", binding.command().name());
        }

        @Test
        void メジャーモードキーマップがグローバルより優先される() {
            var resolver = new KeyResolver();
            var globalKeymap = new Keymap("global");
            globalKeymap.bind(KeyStroke.ctrl('f'), dummyCommand("global-cmd"));
            resolver.addKeymap(globalKeymap);

            var majorKeymap = new Keymap("major");
            majorKeymap.bind(KeyStroke.ctrl('f'), dummyCommand("major-cmd"));

            var entry = resolver.resolveWithBuffer(
                    KeyStroke.ctrl('f'), Optional.empty(), Lists.immutable.empty(), Optional.of(majorKeymap));

            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("major-cmd", binding.command().name());
        }

        @Test
        void どの階層にもないキーはemptyを返す() {
            var resolver = new KeyResolver();
            resolver.addKeymap(new Keymap("global"));

            var entry = resolver.resolveWithBuffer(
                    KeyStroke.ctrl('z'), Optional.empty(), Lists.immutable.empty(), Optional.empty());

            assertTrue(entry.isEmpty());
        }

        @Test
        void バッファローカルにないキーはマイナーモードにフォールバックする() {
            var resolver = new KeyResolver();
            resolver.addKeymap(new Keymap("global"));

            var localKeymap = new Keymap("local");
            // localKeymap にはバインドなし

            var minorKeymap = new Keymap("minor");
            minorKeymap.bind(KeyStroke.ctrl('n'), dummyCommand("minor-next"));

            var entry = resolver.resolveWithBuffer(
                    KeyStroke.ctrl('n'), Optional.of(localKeymap), Lists.immutable.of(minorKeymap), Optional.empty());

            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("minor-next", binding.command().name());
        }

        @Test
        void 複数マイナーモードでは先頭が優先される() {
            var resolver = new KeyResolver();
            resolver.addKeymap(new Keymap("global"));

            var minor1 = new Keymap("minor1");
            minor1.bind(KeyStroke.ctrl('f'), dummyCommand("minor1-cmd"));

            var minor2 = new Keymap("minor2");
            minor2.bind(KeyStroke.ctrl('f'), dummyCommand("minor2-cmd"));

            // minor1が先頭 = 最優先
            var entry = resolver.resolveWithBuffer(
                    KeyStroke.ctrl('f'), Optional.empty(), Lists.immutable.of(minor1, minor2), Optional.empty());

            assertTrue(entry.isPresent());
            var binding = assertInstanceOf(KeymapEntry.CommandBinding.class, entry.get());
            assertEquals("minor1-cmd", binding.command().name());
        }
    }
}
