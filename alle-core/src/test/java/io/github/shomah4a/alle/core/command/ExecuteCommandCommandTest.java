package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExecuteCommandCommandTest {

    @Nested
    class 名前によるコマンド実行 {

        @Test
        void レジストリに登録されたコマンドを名前で実行できる() {
            var registry = new CommandRegistry();
            registry.register(new ForwardCharCommand());
            var execCmd = new ExecuteCommandCommand(registry, new InputHistory());

            var context = TestCommandContextFactory.createDefault();
            context.frame().getActiveWindow().insert("Hello");
            context.frame().getActiveWindow().setPoint(0);

            execCmd.executeByName("forward-char", context).join();

            assertEquals(1, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 未登録のコマンド名を指定すると例外を投げる() {
            var registry = new CommandRegistry();
            var execCmd = new ExecuteCommandCommand(registry, new InputHistory());
            var context = TestCommandContextFactory.createDefault();

            assertThrows(IllegalArgumentException.class, () -> execCmd.executeByName("nonexistent", context));
        }

        @Test
        void 複数コマンドを連続して名前で実行できる() {
            var registry = new CommandRegistry();
            registry.register(new ForwardCharCommand());
            registry.register(new BackwardCharCommand());
            var execCmd = new ExecuteCommandCommand(registry, new InputHistory());

            var context = TestCommandContextFactory.createDefault();
            context.frame().getActiveWindow().insert("Hello");
            context.frame().getActiveWindow().setPoint(0);

            execCmd.executeByName("forward-char", context).join();
            execCmd.executeByName("forward-char", context).join();
            execCmd.executeByName("backward-char", context).join();

            assertEquals(1, context.frame().getActiveWindow().getPoint());
        }
    }

    @Nested
    class インタラクティブ実行 {

        @Test
        void プロンプトで入力したコマンド名を実行する() {
            var registry = new CommandRegistry();
            registry.register(new ForwardCharCommand());
            var execCmd = new ExecuteCommandCommand(registry, new InputHistory());

            var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel()));
            var window = new Window(buffer);
            var minibuffer = new Window(new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel())));
            var frame = new Frame(window, minibuffer);
            var bufferManager = new BufferManager();
            bufferManager.add(buffer);
            window.insert("Hello");
            window.setPoint(0);

            InputPrompter prompter =
                    (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed("forward-char"));
            var context = TestCommandContextFactory.create(frame, bufferManager, prompter);

            execCmd.execute(context).join();

            assertEquals(1, frame.getActiveWindow().getPoint());
        }

        @Test
        void キャンセル時は何も実行しない() {
            var registry = new CommandRegistry();
            registry.register(new ForwardCharCommand());
            var execCmd = new ExecuteCommandCommand(registry, new InputHistory());

            var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel()));
            var window = new Window(buffer);
            var minibuffer = new Window(new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel())));
            var frame = new Frame(window, minibuffer);
            var bufferManager = new BufferManager();
            bufferManager.add(buffer);
            window.insert("Hello");
            window.setPoint(0);

            InputPrompter prompter =
                    (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
            var context = TestCommandContextFactory.create(frame, bufferManager, prompter);

            execCmd.execute(context).join();

            assertEquals(0, frame.getActiveWindow().getPoint());
        }

        @Test
        void 空文字列で確定した場合は何も実行しない() {
            var registry = new CommandRegistry();
            registry.register(new ForwardCharCommand());
            var execCmd = new ExecuteCommandCommand(registry, new InputHistory());

            var context = TestCommandContextFactory.createDefault();
            context.frame().getActiveWindow().insert("Hello");
            context.frame().getActiveWindow().setPoint(0);

            // createDefault uses NOOP_PROMPTER which returns Cancelled
            execCmd.execute(context).join();

            assertEquals(0, context.frame().getActiveWindow().getPoint());
        }

        @Test
        void 存在しないコマンド名で確定した場合は例外を投げる() {
            var registry = new CommandRegistry();
            var execCmd = new ExecuteCommandCommand(registry, new InputHistory());

            var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel()));
            var window = new Window(buffer);
            var minibuffer = new Window(new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel())));
            var frame = new Frame(window, minibuffer);
            var bufferManager = new BufferManager();
            bufferManager.add(buffer);

            InputPrompter prompter =
                    (message, history) -> CompletableFuture.completedFuture(new PromptResult.Confirmed("nonexistent"));
            var context = TestCommandContextFactory.create(frame, bufferManager, prompter);

            var future = execCmd.execute(context);
            assertThrows(Exception.class, future::join);
        }
    }

    @Nested
    class コマンドメタデータ {

        @Test
        void コマンド名はexecute_commandである() {
            var registry = new CommandRegistry();
            var execCmd = new ExecuteCommandCommand(registry, new InputHistory());

            assertEquals("execute-command", execCmd.name());
        }
    }
}
