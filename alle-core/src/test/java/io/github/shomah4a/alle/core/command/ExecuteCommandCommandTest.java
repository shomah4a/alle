package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExecuteCommandCommandTest {

    @Nested
    class 名前によるコマンド実行 {

        @Test
        void レジストリに登録されたコマンドを名前で実行できる() {
            var registry = new CommandRegistry();
            registry.register(new ForwardCharCommand());
            var execCmd = new ExecuteCommandCommand(registry);

            var context = TestCommandContextFactory.createDefault();
            context.frame().getActiveWindow().insert("Hello");
            context.frame().getActiveWindow().setPoint(0);

            execCmd.executeByName("forward-char", context);

            assertEquals(1, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 未登録のコマンド名を指定すると例外を投げる() {
            var registry = new CommandRegistry();
            var execCmd = new ExecuteCommandCommand(registry);
            var context = TestCommandContextFactory.createDefault();

            assertThrows(IllegalArgumentException.class, () -> execCmd.executeByName("nonexistent", context));
        }

        @Test
        void 複数コマンドを連続して名前で実行できる() {
            var registry = new CommandRegistry();
            registry.register(new ForwardCharCommand());
            registry.register(new BackwardCharCommand());
            var execCmd = new ExecuteCommandCommand(registry);

            var context = TestCommandContextFactory.createDefault();
            context.frame().getActiveWindow().insert("Hello");
            context.frame().getActiveWindow().setPoint(0);

            execCmd.executeByName("forward-char", context);
            execCmd.executeByName("forward-char", context);
            execCmd.executeByName("backward-char", context);

            assertEquals(1, context.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class コマンドメタデータ {

        @Test
        void コマンド名はexecute_commandである() {
            var registry = new CommandRegistry();
            var execCmd = new ExecuteCommandCommand(registry);

            assertEquals("execute-command", execCmd.name());
        }
    }
}
