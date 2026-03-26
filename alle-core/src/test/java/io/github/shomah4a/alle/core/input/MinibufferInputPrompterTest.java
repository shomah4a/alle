package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import io.github.shomah4a.alle.core.window.WindowTree;
import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
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
        var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel(), new SettingsRegistry()));
        mainWindow = new Window(buffer);
        minibufferWindow = new Window(
                new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel(), new SettingsRegistry())));
        frame = new Frame(mainWindow, minibufferWindow);
        prompter = new MinibufferInputPrompter(frame);
    }

    @Nested
    class プロンプト表示 {

        @Test
        void プロンプト文字列がミニバッファに挿入される() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            assertEquals("Find file: ", minibufferWindow.getBuffer().getText());
        }

        @Test
        void ミニバッファがアクティブになる() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            assertTrue(frame.isMinibufferActive());
            assertEquals(minibufferWindow, frame.getActiveWindow());
        }

        @Test
        void ポイントがプロンプト文字列の末尾に設定される() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            assertEquals(11, minibufferWindow.getPoint());
        }

        @Test
        void 返却されるfutureは未完了状態である() {
            var future = prompter.prompt("Find file: ", new InputHistory());

            assertFalse(future.isDone());
        }

        @Test
        void ミニバッファにローカルキーマップが設定される() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            assertTrue(minibufferWindow.getBuffer().getLocalKeymap().isPresent());
        }

        @Test
        void プロンプトがアクティブな状態で再度promptを呼ぶと後続がキャンセルされる() {
            var future1 = prompter.prompt("Find file: ", new InputHistory());
            var future2 = prompter.prompt("Save file: ", new InputHistory());

            // 後続プロンプトは即座にCancelledで完了する
            assertTrue(future2.isDone());
            assertTrue(future2.join() instanceof PromptResult.Cancelled);

            // 先行プロンプトはアクティブなまま
            assertFalse(future1.isDone());
            assertEquals("Find file: ", minibufferWindow.getBuffer().getText());
        }

        @Test
        void 確定後は再度promptを呼べる() {
            var unused = prompter.prompt("Find file: ", new InputHistory());
            executeMinibufferKey(KeyStroke.of('\n'));

            var future2 = prompter.prompt("Save file: ", new InputHistory());

            assertFalse(future2.isDone());
            assertEquals("Save file: ", minibufferWindow.getBuffer().getText());
        }

        @Test
        void キャンセル後は再度promptを呼べる() {
            var unused = prompter.prompt("Find file: ", new InputHistory());
            executeMinibufferKey(KeyStroke.ctrl('g'));

            var future2 = prompter.prompt("Save file: ", new InputHistory());

            assertFalse(future2.isDone());
            assertEquals("Save file: ", minibufferWindow.getBuffer().getText());
        }
    }

    @Nested
    class 入力確定 {

        @Test
        void RETでユーザー入力が確定される() {
            var future = prompter.prompt("Find file: ", new InputHistory());

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
            var future = prompter.prompt("Find file: ", new InputHistory());

            executeMinibufferKey(KeyStroke.of('\n'));

            assertTrue(future.isDone());
            var result = future.join();
            assertTrue(result instanceof PromptResult.Confirmed);
            assertEquals("", ((PromptResult.Confirmed) result).value());
        }

        @Test
        void 確定後にミニバッファがクリアされる() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            executeMinibufferKey(KeyStroke.of('\n'));

            assertEquals("", minibufferWindow.getBuffer().getText());
        }

        @Test
        void 確定後にアクティブウィンドウが元に戻る() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            executeMinibufferKey(KeyStroke.of('\n'));

            assertFalse(frame.isMinibufferActive());
            assertEquals(mainWindow, frame.getActiveWindow());
        }

        @Test
        void 確定後にローカルキーマップが解除される() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            executeMinibufferKey(KeyStroke.of('\n'));

            assertTrue(minibufferWindow.getBuffer().getLocalKeymap().isEmpty());
        }
    }

    @Nested
    class キャンセル {

        @Test
        void CgでキャンセルされるとCancelledが返る() {
            var future = prompter.prompt("Find file: ", new InputHistory());

            executeMinibufferKey(KeyStroke.ctrl('g'));

            assertTrue(future.isDone());
            var result = future.join();
            assertTrue(result instanceof PromptResult.Cancelled);
        }

        @Test
        void キャンセル後にミニバッファがクリアされる() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            minibufferWindow.getBuffer().insertText(11, "test.txt");

            executeMinibufferKey(KeyStroke.ctrl('g'));

            assertEquals("", minibufferWindow.getBuffer().getText());
        }

        @Test
        void キャンセル後にアクティブウィンドウが元に戻る() {
            var unused = prompter.prompt("Find file: ", new InputHistory());

            executeMinibufferKey(KeyStroke.ctrl('g'));

            assertFalse(frame.isMinibufferActive());
            assertEquals(mainWindow, frame.getActiveWindow());
        }
    }

    @Nested
    class CommandLoop経由の入力 {

        @Test
        void CommandLoop経由でミニバッファに文字が入力される() {
            var future = prompter.prompt("Find file: ", new InputHistory());

            // CommandLoopを作成してミニバッファのキーマップを使って文字入力
            var resolver = new KeyResolver();
            var bufferManager = new BufferManager();
            var loop = new CommandLoop(
                    () -> Optional.empty(),
                    resolver,
                    frame,
                    bufferManager,
                    prompter,
                    new KillRing(),
                    new MessageBuffer("*Messages*", 100, new SettingsRegistry()),
                    new MessageBuffer("*Warnings*", 100, new SettingsRegistry()),
                    new SettingsRegistry());

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
            var future = prompter.prompt("Find file: ", new InputHistory());

            var resolver = new KeyResolver();
            var bufferManager = new BufferManager();
            var loop = new CommandLoop(
                    () -> Optional.empty(),
                    resolver,
                    frame,
                    bufferManager,
                    prompter,
                    new KillRing(),
                    new MessageBuffer("*Messages*", 100, new SettingsRegistry()),
                    new MessageBuffer("*Warnings*", 100, new SettingsRegistry()),
                    new SettingsRegistry());

            loop.processKey(KeyStroke.of('a'));
            loop.processKey(KeyStroke.of('b'));
            loop.processKey(KeyStroke.of('\n'));

            assertTrue(future.isDone());
            var result = future.join();
            assertTrue(result instanceof PromptResult.Confirmed);
            assertEquals("ab", ((PromptResult.Confirmed) result).value());
        }
    }

    @Nested
    class ヒストリナビゲーション {

        private InputHistory history;

        @BeforeEach
        void setUpHistory() {
            history = new InputHistory();
            history.add("/home/a.txt");
            history.add("/home/b.txt");
            history.add("/home/c.txt");
        }

        @Test
        void Mpで最新の履歴がミニバッファに表示される() {
            var unused = prompter.prompt(
                    "Find file: ", "", history, input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.meta('p'));

            assertEquals("Find file: /home/c.txt", minibufferWindow.getBuffer().getText());
        }

        @Test
        void Mpを複数回押すと古い履歴に遡る() {
            var unused = prompter.prompt(
                    "Find file: ", "", history, input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.meta('p'));
            executeMinibufferKey(KeyStroke.meta('p'));

            assertEquals("Find file: /home/b.txt", minibufferWindow.getBuffer().getText());
        }

        @Test
        void ArrowUpでMpと同じ動作をする() {
            var unused = prompter.prompt(
                    "Find file: ", "", history, input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.of(KeyStroke.ARROW_UP));

            assertEquals("Find file: /home/c.txt", minibufferWindow.getBuffer().getText());
        }

        @Test
        void MnでMpの後に次の履歴に進む() {
            var unused = prompter.prompt(
                    "Find file: ", "", history, input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.meta('p'));
            executeMinibufferKey(KeyStroke.meta('p'));
            executeMinibufferKey(KeyStroke.meta('n'));

            assertEquals("Find file: /home/c.txt", minibufferWindow.getBuffer().getText());
        }

        @Test
        void ArrowDownでMnと同じ動作をする() {
            var unused = prompter.prompt(
                    "Find file: ", "", history, input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.meta('p'));
            executeMinibufferKey(KeyStroke.meta('p'));
            executeMinibufferKey(KeyStroke.of(KeyStroke.ARROW_DOWN));

            assertEquals("Find file: /home/c.txt", minibufferWindow.getBuffer().getText());
        }

        @Test
        void Mnで末尾を超えると元入力に戻る() {
            var unused = prompter.prompt(
                    "Find file: ",
                    "original",
                    history,
                    input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.meta('p'));
            executeMinibufferKey(KeyStroke.meta('n'));

            assertEquals("Find file: original", minibufferWindow.getBuffer().getText());
        }

        @Test
        void 履歴が空の場合Mpで入力が変わらない() {
            var emptyHistory = new InputHistory();
            var unused = prompter.prompt(
                    "Find file: ",
                    "test",
                    emptyHistory,
                    input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.meta('p'));

            assertEquals("Find file: test", minibufferWindow.getBuffer().getText());
        }

        @Test
        void 確定時に履歴に入力が追加される() {
            var unused = prompter.prompt(
                    "Find file: ", "", history, input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            minibufferWindow.getBuffer().insertText(11, "/home/new.txt");
            minibufferWindow.setPoint(24);
            executeMinibufferKey(KeyStroke.of('\n'));

            assertEquals(4, history.size());
            assertEquals("/home/new.txt", history.get(3));
        }

        @Test
        void ヒストリナビゲーション後に入力を編集して確定できる() {
            var unused = prompter.prompt(
                    "Find file: ", "", history, input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.meta('p')); // /home/c.txt

            // ポイント位置を確認して確定
            executeMinibufferKey(KeyStroke.of('\n'));

            // /home/c.txt は既に履歴にあるため重複移動でサイズは3のまま
            assertEquals(3, history.size());
            assertEquals("/home/c.txt", history.get(2));
        }

        @Test
        void ポイントが履歴テキストの末尾に移動する() {
            var unused = prompter.prompt(
                    "Find file: ", "", history, input -> org.eclipse.collections.api.factory.Lists.immutable.empty());

            executeMinibufferKey(KeyStroke.meta('p'));

            // promptLength(11) + "/home/c.txt"(11) = 22
            assertEquals(22, minibufferWindow.getPoint());
        }
    }

    @Nested
    class 補完候補一覧表示 {

        private Completer multiCandidateCompleter;

        @BeforeEach
        void setUpCompleter() {
            // "fo" に対して "foobar", "foobaz" を返す Completer
            multiCandidateCompleter = input -> {
                var all = Lists.immutable.of("foobar", "foobaz", "fooqux");
                return all.select(s -> s.startsWith(input));
            };
        }

        @Test
        void 初回Tabで最長共通プレフィックスまで補完する() {
            var unused = prompter.prompt("Input: ", "", new InputHistory(), multiCandidateCompleter);

            minibufferWindow.getBuffer().insertText(7, "foo");
            minibufferWindow.setPoint(10);
            executeMinibufferKey(KeyStroke.of('\t'));

            assertEquals("Input: foo", minibufferWindow.getBuffer().getText());
            // ウィンドウは分割されていない
            assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
        }

        @Test
        void 補完が進まない再TabでCompletionsウィンドウが表示される() {
            var unused = prompter.prompt("Input: ", "", new InputHistory(), multiCandidateCompleter);

            minibufferWindow.getBuffer().insertText(7, "foo");
            minibufferWindow.setPoint(10);

            // 1回目Tab: 共通プレフィックス "foo" → 進まない
            executeMinibufferKey(KeyStroke.of('\t'));
            // 2回目Tab: lastCommand = "minibuffer-complete"
            executeMinibufferKey(KeyStroke.of('\t'), Optional.of("minibuffer-complete"));

            // ウィンドウが分割されている
            var split = assertInstanceOf(WindowTree.Split.class, frame.getWindowTree());
            var completionsWindow = ((WindowTree.Leaf) split.second()).window();
            var completionsText = completionsWindow.getBuffer().getText();
            assertTrue(completionsText.contains("foobar"));
            assertTrue(completionsText.contains("foobaz"));
            assertTrue(completionsText.contains("fooqux"));
        }

        @Test
        void 候補が一件ならそのまま補完確定する() {
            Completer singleCompleter = input -> Lists.immutable.of("foobar").select(s -> s.startsWith(input));
            var unused = prompter.prompt("Input: ", "", new InputHistory(), singleCompleter);

            minibufferWindow.getBuffer().insertText(7, "foo");
            minibufferWindow.setPoint(10);
            executeMinibufferKey(KeyStroke.of('\t'));

            assertEquals("Input: foobar", minibufferWindow.getBuffer().getText());
        }

        @Test
        void 確定時にCompletionsウィンドウが閉じる() {
            var unused = prompter.prompt("Input: ", "", new InputHistory(), multiCandidateCompleter);

            minibufferWindow.getBuffer().insertText(7, "foo");
            minibufferWindow.setPoint(10);
            executeMinibufferKey(KeyStroke.of('\t'));
            executeMinibufferKey(KeyStroke.of('\t'), Optional.of("minibuffer-complete"));

            // *Completions* ウィンドウが存在する
            assertInstanceOf(WindowTree.Split.class, frame.getWindowTree());

            // RETで確定
            executeMinibufferKey(KeyStroke.of('\n'));

            // *Completions* ウィンドウが閉じている
            assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
        }

        @Test
        void キャンセル時にCompletionsウィンドウが閉じる() {
            var unused = prompter.prompt("Input: ", "", new InputHistory(), multiCandidateCompleter);

            minibufferWindow.getBuffer().insertText(7, "foo");
            minibufferWindow.setPoint(10);
            executeMinibufferKey(KeyStroke.of('\t'));
            executeMinibufferKey(KeyStroke.of('\t'), Optional.of("minibuffer-complete"));

            // C-gでキャンセル
            executeMinibufferKey(KeyStroke.ctrl('g'));

            assertInstanceOf(WindowTree.Leaf.class, frame.getWindowTree());
        }

        @Test
        void activeWindowがミニバッファのまま維持される() {
            var unused = prompter.prompt("Input: ", "", new InputHistory(), multiCandidateCompleter);

            minibufferWindow.getBuffer().insertText(7, "foo");
            minibufferWindow.setPoint(10);
            executeMinibufferKey(KeyStroke.of('\t'));
            executeMinibufferKey(KeyStroke.of('\t'), Optional.of("minibuffer-complete"));

            assertSame(minibufferWindow, frame.getActiveWindow());
        }
    }

    /**
     * ミニバッファのローカルキーマップからコマンドを検索して実行する。
     */
    private void executeMinibufferKey(KeyStroke keyStroke) {
        executeMinibufferKey(keyStroke, Optional.empty());
    }

    /**
     * ミニバッファのローカルキーマップからコマンドを検索して実行する。
     * lastCommandを指定可能。
     */
    private void executeMinibufferKey(KeyStroke keyStroke, Optional<String> lastCommand) {
        var keymapOpt = minibufferWindow.getBuffer().getLocalKeymap();
        assertTrue(keymapOpt.isPresent(), "ミニバッファにローカルキーマップが設定されていません");
        var entryOpt = keymapOpt.get().lookup(keyStroke);
        assertTrue(entryOpt.isPresent(), "キー " + keyStroke + " に対するバインドがありません");
        var entry = entryOpt.get();
        if (entry instanceof io.github.shomah4a.alle.core.keybind.KeymapEntry.CommandBinding binding) {
            var context = new CommandContext(
                    frame,
                    new BufferManager(),
                    frame.getActiveWindow(),
                    prompter,
                    Optional.of(keyStroke),
                    Optional.of(binding.command().name()),
                    lastCommand,
                    new io.github.shomah4a.alle.core.command.KillRing(),
                    new io.github.shomah4a.alle.core.buffer.MessageBuffer("*Messages*", 100, new SettingsRegistry()),
                    new io.github.shomah4a.alle.core.buffer.MessageBuffer("*Warnings*", 100, new SettingsRegistry()),
                    new SettingsRegistry());
            binding.command().execute(context).join();
        }
    }
}
