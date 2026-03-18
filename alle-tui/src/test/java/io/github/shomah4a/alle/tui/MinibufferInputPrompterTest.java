package io.github.shomah4a.alle.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MinibufferInputPrompterTest {

    private Frame frame;
    private Window mainWindow;
    private Window minibufferWindow;
    private MinibufferInputPrompter prompter;

    @BeforeEach
    void setUp() {
        var buffer = new EditableBuffer("test", new GapTextModel());
        mainWindow = new Window(buffer);
        minibufferWindow = new Window(new EditableBuffer("*Minibuffer*", new GapTextModel()));
        frame = new Frame(mainWindow, minibufferWindow);
        prompter = new MinibufferInputPrompter(frame);
    }

    @Nested
    class プロンプト表示 {

        @Test
        void プロンプト文字列がミニバッファに挿入される() {
            prompter.prompt("Find file: ");

            assertEquals("Find file: ", minibufferWindow.getBuffer().getText());
        }

        @Test
        void ミニバッファがアクティブになる() {
            prompter.prompt("Find file: ");

            assertTrue(frame.isMinibufferActive());
            assertEquals(minibufferWindow, frame.getActiveWindow());
        }

        @Test
        void ポイントがプロンプト文字列の末尾に設定される() {
            prompter.prompt("Find file: ");

            assertEquals(11, minibufferWindow.getPoint());
        }

        @Test
        void 返却されるfutureは未完了状態である() {
            var future = prompter.prompt("Find file: ");

            assertFalse(future.isDone());
        }

        @Test
        void ミニバッファにローカルキーマップが設定される() {
            prompter.prompt("Find file: ");

            assertTrue(minibufferWindow.getBuffer().getLocalKeymap().isPresent());
        }

        @Test
        void プロンプトがアクティブな状態で再度promptを呼ぶと後続がキャンセルされる() {
            var future1 = prompter.prompt("Find file: ");
            var future2 = prompter.prompt("Save file: ");

            // 後続プロンプトは即座にCancelledで完了する
            assertTrue(future2.isDone());
            assertTrue(future2.join() instanceof PromptResult.Cancelled);

            // 先行プロンプトはアクティブなまま
            assertFalse(future1.isDone());
            assertEquals("Find file: ", minibufferWindow.getBuffer().getText());
        }

        @Test
        void 確定後は再度promptを呼べる() {
            prompter.prompt("Find file: ");
            executeMinibufferKey(KeyStroke.of('\n'));

            var future2 = prompter.prompt("Save file: ");

            assertFalse(future2.isDone());
            assertEquals("Save file: ", minibufferWindow.getBuffer().getText());
        }

        @Test
        void キャンセル後は再度promptを呼べる() {
            prompter.prompt("Find file: ");
            executeMinibufferKey(KeyStroke.ctrl('g'));

            var future2 = prompter.prompt("Save file: ");

            assertFalse(future2.isDone());
            assertEquals("Save file: ", minibufferWindow.getBuffer().getText());
        }
    }

    @Nested
    class 入力確定 {

        @Test
        void RETでユーザー入力が確定される() {
            var future = prompter.prompt("Find file: ");

            // ミニバッファにテキストを追加（プロンプト後に入力）
            minibufferWindow.getBuffer().insertText(11, "test.txt");
            minibufferWindow.setPoint(19);

            // RETキーでConfirmコマンドを実行
            executeMinibufferKey(KeyStroke.of('\n'));

            assertTrue(future.isDone());
            var result = future.join();
            assertTrue(result instanceof PromptResult.Confirmed);
            assertEquals("test.txt", ((PromptResult.Confirmed) result).value());
        }

        @Test
        void 入力なしでRETを押すと空文字列で確定される() {
            var future = prompter.prompt("Find file: ");

            executeMinibufferKey(KeyStroke.of('\n'));

            assertTrue(future.isDone());
            var result = future.join();
            assertTrue(result instanceof PromptResult.Confirmed);
            assertEquals("", ((PromptResult.Confirmed) result).value());
        }

        @Test
        void 確定後にミニバッファがクリアされる() {
            prompter.prompt("Find file: ");

            executeMinibufferKey(KeyStroke.of('\n'));

            assertEquals("", minibufferWindow.getBuffer().getText());
        }

        @Test
        void 確定後にアクティブウィンドウが元に戻る() {
            prompter.prompt("Find file: ");

            executeMinibufferKey(KeyStroke.of('\n'));

            assertFalse(frame.isMinibufferActive());
            assertEquals(mainWindow, frame.getActiveWindow());
        }

        @Test
        void 確定後にローカルキーマップが解除される() {
            prompter.prompt("Find file: ");

            executeMinibufferKey(KeyStroke.of('\n'));

            assertTrue(minibufferWindow.getBuffer().getLocalKeymap().isEmpty());
        }
    }

    @Nested
    class キャンセル {

        @Test
        void CgでキャンセルされるとCancelledが返る() {
            var future = prompter.prompt("Find file: ");

            executeMinibufferKey(KeyStroke.ctrl('g'));

            assertTrue(future.isDone());
            var result = future.join();
            assertTrue(result instanceof PromptResult.Cancelled);
        }

        @Test
        void キャンセル後にミニバッファがクリアされる() {
            prompter.prompt("Find file: ");

            minibufferWindow.getBuffer().insertText(11, "test.txt");

            executeMinibufferKey(KeyStroke.ctrl('g'));

            assertEquals("", minibufferWindow.getBuffer().getText());
        }

        @Test
        void キャンセル後にアクティブウィンドウが元に戻る() {
            prompter.prompt("Find file: ");

            executeMinibufferKey(KeyStroke.ctrl('g'));

            assertFalse(frame.isMinibufferActive());
            assertEquals(mainWindow, frame.getActiveWindow());
        }
    }

    @Nested
    class CommandLoop経由の入力 {

        @Test
        void CommandLoop経由でミニバッファに文字が入力される() {
            var future = prompter.prompt("Find file: ");

            // CommandLoopを作成してミニバッファのキーマップを使って文字入力
            var resolver = new KeyResolver();
            var bufferManager = new BufferManager();
            var loop = new CommandLoop(() -> Optional.empty(), resolver, frame, bufferManager, prompter);

            // ミニバッファがアクティブな状態でキー入力
            loop.processKey(KeyStroke.of('t'));
            loop.processKey(KeyStroke.of('e'));
            loop.processKey(KeyStroke.of('s'));
            loop.processKey(KeyStroke.of('t'));

            assertEquals("Find file: test", minibufferWindow.getBuffer().getText());
            assertFalse(future.isDone());
        }

        @Test
        void CommandLoop経由で入力後にRETで確定される() {
            var future = prompter.prompt("Find file: ");

            var resolver = new KeyResolver();
            var bufferManager = new BufferManager();
            var loop = new CommandLoop(() -> Optional.empty(), resolver, frame, bufferManager, prompter);

            loop.processKey(KeyStroke.of('a'));
            loop.processKey(KeyStroke.of('b'));
            loop.processKey(KeyStroke.of('\n'));

            assertTrue(future.isDone());
            var result = future.join();
            assertTrue(result instanceof PromptResult.Confirmed);
            assertEquals("ab", ((PromptResult.Confirmed) result).value());
        }
    }

    /**
     * ミニバッファのローカルキーマップからコマンドを検索して実行する。
     */
    private void executeMinibufferKey(KeyStroke keyStroke) {
        var keymapOpt = minibufferWindow.getBuffer().getLocalKeymap();
        assertTrue(keymapOpt.isPresent(), "ミニバッファにローカルキーマップが設定されていません");
        var entryOpt = keymapOpt.get().lookup(keyStroke);
        assertTrue(entryOpt.isPresent(), "キー " + keyStroke + " に対するバインドがありません");
        var entry = entryOpt.get();
        if (entry instanceof io.github.shomah4a.alle.core.keybind.KeymapEntry.CommandBinding binding) {
            var windowActor = new io.github.shomah4a.alle.core.window.WindowActor(frame.getActiveWindow());
            var context = new CommandContext(
                    frame,
                    new BufferManager(),
                    windowActor,
                    prompter,
                    Optional.of(keyStroke),
                    Optional.empty(),
                    Optional.empty(),
                    new io.github.shomah4a.alle.core.command.KillRing());
            binding.command().execute(context).join();
        }
    }
}
