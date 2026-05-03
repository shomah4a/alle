package io.github.shomah4a.alle.core.mode.modes.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * ShellBufferModel の E2E テスト。
 * 実際の PTY 出力パターン（ANSI エスケープシーケンス、CR、bracket paste mode）を
 * シミュレートして動作を検証する。
 */
class ShellEndToEndTest {

    private static BufferFacade createBuffer() {
        var settings = new SettingsRegistry();
        var textBuffer = new TextBuffer("*shell*", new GapTextModel(), settings);
        return new BufferFacade(textBuffer);
    }

    @Nested
    class プロンプト表示 {

        @Test
        void 起動直後のプロンプトが表示される() {
            var buffer = createBuffer();
            var latch = new CountDownLatch(1);
            var model = new ShellBufferModel(buffer, latch::countDown);
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            // PTY経由のbashプロンプト出力をシミュレート
            // 改行なし（プロンプトはユーザー入力を待つため改行で終わらない）
            model.appendOutput("\u001b[?2004hbash-5.2$ \u001b[?2004l\r");

            String text = buffer.getText();
            assertEquals("bash-5.2$ ", text);
            assertバッファにエスケープ文字を含まない(text);
        }

        @Test
        void プロンプトの後にユーザー入力位置がある() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            model.appendOutput("\u001b[?2004hbash-5.2$ \u001b[?2004l\r");

            // inputStartPositionはプロンプトの末尾
            assertEquals("bash-5.2$ ".length(), model.getInputStartPosition());
        }
    }

    @Nested
    class コマンド実行フロー {

        @Test
        void プロンプト表示_入力送信_出力表示_次のプロンプト表示の一連の流れ() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            // 1. 初期プロンプト
            model.appendOutput("\u001b[?2004hbash-5.2$ \u001b[?2004l\r");
            assertEquals("bash-5.2$ ", buffer.getText());

            // 2. ユーザーが "echo hello" と入力
            buffer.insertText(buffer.length(), "echo hello");
            assertEquals("bash-5.2$ echo hello", buffer.getText());

            // 3. RET で送信
            model.sendInput();
            assertEquals("echo hello", process.getSentInputs().get(0));

            // 4. コマンド出力が返ってくる（PTY形式）
            model.appendOutput("hello\r");
            // 5. 次のプロンプト
            model.appendOutput("\u001b[?2004hbash-5.2$ \u001b[?2004l\r");

            String text = buffer.getText();
            assertTrue(text.contains("hello"));
            assertTrue(text.contains("bash-5.2$ "));
            assertバッファにエスケープ文字を含まない(text);
            // 末尾がプロンプトで終わる
            assertTrue(text.endsWith("bash-5.2$ "));
        }

        @Test
        void 色付き出力が正しくパースされる() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            // プロンプト
            model.appendOutput("$ \r");

            // ls --color の出力をシミュレート（赤色テキスト）
            model.appendOutput("\u001b[31mred_file\u001b[0m  normal_file\r");

            String text = buffer.getText();
            assertTrue(text.contains("red_file"));
            assertTrue(text.contains("normal_file"));
            assertバッファにエスケープ文字を含まない(text);
        }

        @Test
        void OSCシーケンスが除去される() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            // ターミナルタイトル設定 + プロンプト
            model.appendOutput("\u001b]0;user@host:~\u0007$ \r");

            String text = buffer.getText();
            assertEquals("$ ", text);
            assertバッファにエスケープ文字を含まない(text);
        }

        @Test
        void CRのみの空行はスキップされる() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            model.appendOutput("$ \r");
            model.appendOutput("\r"); // 空行（PTYの \r\r\n の \r 部分）
            model.appendOutput("hello\r");

            String text = buffer.getText();
            assertFalse(text.contains("\n\n"));
            assertバッファにエスケープ文字を含まない(text);
        }

        @Test
        void CSIシーケンスが途中で分割されても正しく処理される() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            // ESCが単独で来るケース（available()==0で部分フラッシュされる場合）
            model.appendOutput("\u001b[?2004hbash$ \u001b[?2004l\r");

            String text = buffer.getText();
            assertEquals("bash$ ", text);
            assertバッファにエスケープ文字を含まない(text);
        }

        @Test
        void ESC文字だけが単独で来た場合もバッファに残らない() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            // available()==0 で ESC 1文字だけがフラッシュされたケース
            model.appendOutput("\u001b");
            // 続きのシーケンスが次のフラッシュで来る
            model.appendOutput("[?2004hbash$ \u001b[?2004l\r");

            String text = buffer.getText();
            assertバッファにエスケープ文字を含まない(text);
        }

        @Test
        void SGRの途中で分割されても色が正しく適用される() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            // ESC[31m が "ESC" と "[31mred" に分割されたケース
            model.appendOutput("\u001b");
            model.appendOutput("[31mred\u001b[0m\r");

            String text = buffer.getText();
            assertEquals("red", text);
            assertバッファにエスケープ文字を含まない(text);
        }
    }

    @Nested
    class プロセス終了 {

        @Test
        void プロセス終了メッセージが表示される() {
            var buffer = createBuffer();
            var model = new ShellBufferModel(buffer, () -> {});
            var process = new StubInteractiveShellProcess();
            model.setProcess(process);

            model.appendOutput("$ \r");
            model.markProcessFinished();

            assertTrue(buffer.getText().contains("Process shell finished"));
            assertTrue(model.isProcessFinished());
        }
    }

    private static void assertバッファにエスケープ文字を含まない(String text) {
        assertFalse(text.contains("\u001b"), "バッファにESC文字が含まれています: " + text);
        assertFalse(text.contains("\r"), "バッファにCR文字が含まれています");
    }
}
