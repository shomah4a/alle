package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CommandRegistryTest {

    private static Command stubCommand(String name) {
        return new Command() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void execute(CommandContext ctx) {}
        };
    }

    @Nested
    class コマンド登録 {

        @Test
        void コマンドを名前で登録して検索できる() {
            var registry = new CommandRegistry();
            var command = stubCommand("forward-char");
            registry.register(command);

            var result = registry.lookup("forward-char");

            assertTrue(result.isPresent());
            assertEquals(command, result.get());
        }

        @Test
        void 同名コマンドの二重登録は例外を投げる() {
            var registry = new CommandRegistry();
            registry.register(stubCommand("forward-char"));

            assertThrows(IllegalStateException.class, () -> registry.register(stubCommand("forward-char")));
        }

        @Test
        void 未登録のコマンド名を検索するとemptyを返す() {
            var registry = new CommandRegistry();

            var result = registry.lookup("nonexistent");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class コマンド一覧 {

        @Test
        void 登録済みコマンド名の一覧を取得できる() {
            var registry = new CommandRegistry();
            registry.register(stubCommand("forward-char"));
            registry.register(stubCommand("backward-char"));

            var names = registry.registeredNames();

            assertEquals(2, names.size());
            assertTrue(names.contains("forward-char"));
            assertTrue(names.contains("backward-char"));
        }

        @Test
        void 空のレジストリは空の一覧を返す() {
            var registry = new CommandRegistry();

            var names = registry.registeredNames();

            assertEquals(0, names.size());
        }
    }

    @Nested
    class インスタンス同一性 {

        @Test
        void lookupで取得したインスタンスは登録時と同一である() {
            var registry = new CommandRegistry();
            var command = stubCommand("test-command");
            registry.register(command);

            var result = registry.lookup("test-command");

            assertTrue(result.isPresent());
            assertTrue(command == result.get(), "レジストリから取得したインスタンスは登録時と同一であるべき");
        }
    }
}
