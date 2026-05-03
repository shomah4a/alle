package io.github.shomah4a.alle.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.input.Completer;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
import io.github.shomah4a.alle.core.setting.Setting;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.syntax.SyntaxAnalyzerRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.FrameLayoutStore;
import io.github.shomah4a.alle.core.window.Window;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EditorFacadeTest {

    private EditorFacade facade;
    private Frame frame;
    private TextBuffer buffer;
    private MessageBuffer messageBuffer;

    @BeforeEach
    void setUp() {
        var settings = new SettingsRegistry();
        buffer = new TextBuffer("test.py", new GapTextModel(), settings);
        var bufferFacade = new BufferFacade(buffer);
        var scratchBuffer = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), settings));
        var window = new Window(bufferFacade);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settings)));
        frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(bufferFacade);
        bufferManager.add(scratchBuffer);
        messageBuffer = new MessageBuffer("*Messages*", 100, settings);
        facade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new),
                new SyntaxAnalyzerRegistry(),
                new FrameLayoutStore(),
                bufferManager,
                (message, history) -> {
                    throw new UnsupportedOperationException();
                },
                settings);
    }

    @Test
    void アクティブウィンドウのファサードを取得できる() {
        WindowFacade win = facade.activeWindow();
        assertEquals(0, win.point());
    }

    @Test
    void カレントバッファのファサードを取得できる() {
        io.github.shomah4a.alle.script.BufferFacade buf = facade.currentBuffer();
        assertEquals("test.py", buf.name());
    }

    @Test
    void ウィンドウファサード経由でテキストを挿入できる() {
        WindowFacade win = facade.activeWindow();
        win.insert("hello");
        assertEquals("hello", buffer.getText());
    }

    @Test
    void ウィンドウファサード経由でカーソルを移動できる() {
        WindowFacade win = facade.activeWindow();
        win.insert("hello world");
        win.gotoChar(5);
        assertEquals(5, win.point());
    }

    @Test
    void バッファファサード経由で行数を取得できる() {
        buffer.insertText(0, "line1\nline2\nline3");
        io.github.shomah4a.alle.script.BufferFacade buf = facade.currentBuffer();
        assertEquals(3, buf.lineCount());
    }

    @Test
    void メッセージを表示できる() {
        facade.message("test message");
        assertEquals("test message", messageBuffer.lineText(0));
    }

    @Test
    void フレーム状態を保存して存在確認できる() {
        assertFalse(facade.hasFrameState("layout1"));
        facade.saveFrameState("layout1");
        assertTrue(facade.hasFrameState("layout1"));
    }

    @Test
    void 保存したフレーム状態を復元できる() {
        facade.saveFrameState("layout1");
        assertTrue(facade.restoreFrameState("layout1"));
    }

    @Test
    void 存在しないフレーム状態の復元はfalseを返す() {
        assertFalse(facade.restoreFrameState("nonexistent"));
    }

    @Test
    void フレーム状態の保存は同名で上書きできる() {
        facade.saveFrameState("layout1");
        facade.saveFrameState("layout1");
        assertTrue(facade.hasFrameState("layout1"));
        assertTrue(facade.restoreFrameState("layout1"));
    }

    @Test
    void promptはInputPrompterに委譲してCompletableFutureを返す() {
        var confirmed = new PromptResult.Confirmed("test-input");
        var stubFacade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new),
                new SyntaxAnalyzerRegistry(),
                new FrameLayoutStore(),
                new BufferManager(),
                (message, history1) -> CompletableFuture.completedFuture(confirmed),
                new SettingsRegistry());
        var history = stubFacade.createInputHistory();
        var future = stubFacade.prompt("Enter: ", history);
        assertTrue(future.isDone());
        var result = future.join();
        assertInstanceOf(PromptResult.Confirmed.class, result);
        assertEquals("test-input", ((PromptResult.Confirmed) result).value());
    }

    @Test
    void 初期値付きpromptはInputPrompterの4引数オーバーロードに委譲する() {
        var confirmed = new PromptResult.Confirmed("edited-value");
        var stubPrompter = new InputPrompter() {
            @Override
            public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
                throw new UnsupportedOperationException("2引数版が呼ばれるべきではない");
            }

            @Override
            public CompletableFuture<PromptResult> prompt(
                    String message, String initialValue, InputHistory history, Completer completer) {
                assertEquals("initial", initialValue);
                return CompletableFuture.completedFuture(confirmed);
            }
        };
        var stubFacade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new),
                new SyntaxAnalyzerRegistry(),
                new FrameLayoutStore(),
                new BufferManager(),
                stubPrompter,
                new SettingsRegistry());
        var history = stubFacade.createInputHistory();
        var future = stubFacade.prompt("Enter: ", "initial", history);
        assertTrue(future.isDone());
        var result = future.join();
        assertInstanceOf(PromptResult.Confirmed.class, result);
        assertEquals("edited-value", ((PromptResult.Confirmed) result).value());
    }

    @Test
    void createInputHistoryは新しいインスタンスを返す() {
        var history = facade.createInputHistory();
        assertEquals(0, history.size());
        history.add("entry1");
        assertEquals(1, history.size());
    }

    @Test
    void setSettingで値を設定しgetSettingで取得できる() {
        var settings = new SettingsRegistry();
        settings.register(Setting.of("completion-ignore-case", Boolean.class, false));
        var stubFacade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new),
                new SyntaxAnalyzerRegistry(),
                new FrameLayoutStore(),
                new BufferManager(),
                (message, history) -> {
                    throw new UnsupportedOperationException();
                },
                settings);

        stubFacade.setSetting("completion-ignore-case", true);

        assertEquals(true, stubFacade.getSetting("completion-ignore-case"));
    }

    @Test
    void getSettingはグローバル未設定ならデフォルト値を返す() {
        var settings = new SettingsRegistry();
        settings.register(Setting.of("completion-ignore-case", Boolean.class, false));
        var stubFacade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new),
                new SyntaxAnalyzerRegistry(),
                new FrameLayoutStore(),
                new BufferManager(),
                (message, history) -> {
                    throw new UnsupportedOperationException();
                },
                settings);

        assertEquals(false, stubFacade.getSetting("completion-ignore-case"));
    }

    @Test
    void removeSettingでグローバル値を解除しデフォルトに戻る() {
        var settings = new SettingsRegistry();
        settings.register(Setting.of("completion-ignore-case", Boolean.class, false));
        var stubFacade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new),
                new SyntaxAnalyzerRegistry(),
                new FrameLayoutStore(),
                new BufferManager(),
                (message, history) -> {
                    throw new UnsupportedOperationException();
                },
                settings);
        stubFacade.setSetting("completion-ignore-case", true);

        stubFacade.removeSetting("completion-ignore-case");

        assertEquals(false, stubFacade.getSetting("completion-ignore-case"));
    }

    @Test
    void setSettingは未登録キーで例外を投げる() {
        var settings = new SettingsRegistry();
        var stubFacade = new EditorFacade(
                frame,
                messageBuffer,
                new CommandRegistry(),
                new Keymap("global"),
                new ModeRegistry(),
                new AutoModeMap(TextMode::new),
                new SyntaxAnalyzerRegistry(),
                new FrameLayoutStore(),
                new BufferManager(),
                (message, history) -> {
                    throw new UnsupportedOperationException();
                },
                settings);

        assertThrows(IllegalArgumentException.class, () -> stubFacade.setSetting("unknown", true));
    }
}
