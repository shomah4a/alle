package io.github.shomah4a.alle.core.keybind;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.command.Command;
import io.github.shomah4a.alle.core.command.CommandContext;
import java.util.concurrent.CompletableFuture;
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
}
