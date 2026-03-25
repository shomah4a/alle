package io.github.shomah4a.alle.core.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HandleErrorTest {

    private MessageBuffer messageBuffer;
    private MessageBuffer warningBuffer;
    private CommandContext context;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new EditableBuffer("test", new GapTextModel()));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new EditableBuffer("*Minibuffer*", new GapTextModel())));
        var frame = new Frame(window, minibuffer);
        messageBuffer = new MessageBuffer("*Messages*", 100);
        warningBuffer = new MessageBuffer("*Warnings*", 100);
        context = new CommandContext(
                frame,
                new BufferManager(),
                window,
                (msg, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled()),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                messageBuffer,
                warningBuffer);
    }

    @Test
    void エコーエリアにメッセージが表示される() {
        context.handleError("テストエラー", new RuntimeException("test"));

        assertTrue(messageBuffer.isShowingMessage());
        assertEquals("テストエラー", messageBuffer.getLastMessage().orElse(""));
    }

    @Test
    void warningBufferにメッセージが書き込まれる() {
        context.handleError("テストエラー", new RuntimeException("test"));

        assertTrue(warningBuffer.lineCount() > 1);
        assertEquals("テストエラー", warningBuffer.lineText(0));
    }

    @Test
    void warningBufferにスタックトレースが書き込まれる() {
        var exception = new RuntimeException("test exception");
        context.handleError("テストエラー", exception);

        // スタックトレースの2行目以降にはat行が含まれる
        boolean hasStackTrace = false;
        for (int i = 1; i < warningBuffer.lineCount(); i++) {
            if (warningBuffer.lineText(i).contains("at ")) {
                hasStackTrace = true;
                break;
            }
        }
        assertTrue(hasStackTrace, "スタックトレースにat行が含まれていません");
    }

    @Test
    void warningBufferにスタックトレースの例外メッセージが含まれる() {
        var exception = new RuntimeException("test exception message");
        context.handleError("テストエラー", exception);

        // スタックトレースの最初の行（メッセージの次）に例外クラス名とメッセージが含まれる
        boolean hasExceptionMessage = false;
        for (int i = 0; i < warningBuffer.lineCount(); i++) {
            if (warningBuffer.lineText(i).contains("test exception message")) {
                hasExceptionMessage = true;
                break;
            }
        }
        assertTrue(hasExceptionMessage, "例外メッセージが*Warnings*に含まれていません");
    }
}
