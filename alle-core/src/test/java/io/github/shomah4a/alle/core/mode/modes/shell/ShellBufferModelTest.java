package io.github.shomah4a.alle.core.mode.modes.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShellBufferModelTest {

    private static BufferFacade createBuffer() {
        var settings = new SettingsRegistry();
        var textBuffer = new TextBuffer("*shell*", new GapTextModel(), settings);
        return new BufferFacade(textBuffer);
    }

    @Nested
    class appendOutput {

        @Test
        void 出力がバッファに追記される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("hello");

            assertEquals("hello\n", buffer.getText());
        }

        @Test
        void 出力の後にinputStartPositionが更新される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("hello");

            assertEquals(6, model.getInputStartPosition()); // "hello\n".length()
        }

        @Test
        void 複数行の出力が順次追記される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("line1");
            model.appendOutput("line2");

            assertEquals("line1\nline2\n", buffer.getText());
            assertEquals(12, model.getInputStartPosition());
        }

        @Test
        void 出力領域がreadOnlyに設定される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("hello");

            assertTrue(buffer.isReadOnlyAt(0));
            assertTrue(buffer.isReadOnlyAt(4)); // 'o' の位置
        }

        @Test
        void ユーザー入力がある状態で出力が到着した場合に入力が保持される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            // プロンプトの後にユーザーが入力中
            model.appendOutput("$ ");
            buffer.insertText(buffer.length(), "ls -la");

            // 出力が到着
            model.appendOutput("output line");

            String text = buffer.getText();
            assertTrue(text.contains("output line"));
            assertTrue(text.endsWith("ls -la"));
        }

        @Test
        void ユーザー入力の前に出力が挿入される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("prompt");
            buffer.insertText(buffer.length(), "user input");

            model.appendOutput("new output");

            String text = buffer.getText();
            int outputPos = text.indexOf("new output");
            int inputPos = text.indexOf("user input");
            assertTrue(outputPos < inputPos);
        }
    }

    @Nested
    class getCurrentInput {

        @Test
        void 入力がない場合は空文字列を返す() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            assertEquals("", model.getCurrentInput());
        }

        @Test
        void ユーザーが入力したテキストを返す() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("$ ");
            buffer.insertText(buffer.length(), "echo hello");

            assertEquals("echo hello", model.getCurrentInput());
        }
    }

    @Nested
    class sendInput {

        @Test
        void 入力がプロセスに送信される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("$ ");
            buffer.insertText(buffer.length(), "echo hello");

            model.sendInput();

            assertEquals(1, process.getSentInputs().size());
            assertEquals("echo hello", process.getSentInputs().get(0));
        }

        @Test
        void 送信後にinputStartPositionがバッファ末尾に更新される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("$ ");
            buffer.insertText(buffer.length(), "echo hello");

            model.sendInput();

            assertEquals(buffer.length(), model.getInputStartPosition());
        }

        @Test
        void 送信後の入力領域がreadOnlyに設定される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.appendOutput("$ ");
            buffer.insertText(buffer.length(), "echo hello");

            model.sendInput();

            // 送信したテキスト部分がread-onlyになる
            assertTrue(buffer.isReadOnlyAt(0));
        }

        @Test
        void プロセス終了後は送信しない() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.markProcessFinished();
            buffer.insertText(buffer.length(), "should not send");

            model.sendInput();

            assertEquals(0, process.getSentInputs().size());
        }
    }

    @Nested
    class markProcessFinished {

        @Test
        void プロセス終了後はisProcessFinishedがtrueを返す() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            assertFalse(model.isProcessFinished());

            model.markProcessFinished();

            assertTrue(model.isProcessFinished());
        }

        @Test
        void 終了メッセージがバッファに追記される() {
            var buffer = createBuffer();
            var process = new StubInteractiveShellProcess();
            var model = new ShellBufferModel(buffer, process);

            model.markProcessFinished();

            assertTrue(buffer.getText().contains("Process shell finished"));
        }
    }
}
