package io.github.shomah4a.alle.core.command.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandContext;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.command.CommandResolver;
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.command.NoOpOverridingKeymapController;
import io.github.shomah4a.alle.core.input.Completer;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.input.ShellCommandExecutor;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.io.PathOpenService;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ShellCommandCommandTest {

    private static InputPrompter queuePrompter(String... responses) {
        var queue = new LinkedBlockingQueue<String>();
        for (String r : responses) {
            queue.add(r);
        }
        return new InputPrompter() {
            @Override
            public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
                String value = queue.poll();
                if (value == null) {
                    return CompletableFuture.completedFuture(new PromptResult.Cancelled());
                }
                return CompletableFuture.completedFuture(new PromptResult.Confirmed(value));
            }

            @Override
            public CompletableFuture<PromptResult> prompt(
                    String message, String initialValue, InputHistory history, Completer completer) {
                return prompt(message, history);
            }
        };
    }

    private static StubExecutor createExecutor() {
        return new StubExecutor();
    }

    private record TestSetup(CommandContext context, ShellCommandCommand command, StubExecutor executor, Frame frame) {}

    private static TestSetup setup(InputPrompter prompter) {
        return setup(prompter, createExecutor());
    }

    private static TestSetup setup(InputPrompter prompter, StubExecutor executor) {
        var settings = new SettingsRegistry();
        var bufferFacade = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), settings));
        bufferFacade.setFilePath(Path.of("/home/user/project/file.txt"));
        var window = new Window(bufferFacade);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), settings)));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(bufferFacade);

        var registry = new CommandRegistry();
        var context = new CommandContext(
                frame,
                bufferManager,
                window,
                prompter,
                Lists.immutable.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, settings),
                new MessageBuffer("*Warnings*", 100, settings),
                settings,
                new CommandResolver(registry),
                new NoOpOverridingKeymapController(),
                new PathOpenService(
                        new BufferIO(
                                source -> {
                                    throw new java.io.IOException("stub");
                                },
                                destination -> {
                                    throw new java.io.IOException("stub");
                                },
                                settings),
                        new AutoModeMap(TextMode::new),
                        new ModeRegistry(),
                        settings,
                        path -> false,
                        (pathString, bm, f) -> {}));

        var shellHistory = new InputHistory();
        var command = new ShellCommandCommand(executor, shellHistory);

        return new TestSetup(context, command, executor, frame);
    }

    @Nested
    class キャンセルと空入力 {

        @Test
        void キャンセル時はコマンドが実行されない() {
            var ts = setup(queuePrompter());
            ts.command().execute(ts.context()).join();
            assertTrue(ts.executor().executedCommands.isEmpty());
        }

        @Test
        void 空コマンド入力時はコマンドが実行されない() {
            var ts = setup(queuePrompter("  "));
            ts.command().execute(ts.context()).join();
            assertTrue(ts.executor().executedCommands.isEmpty());
        }
    }

    @Nested
    class コマンド実行 {

        @Test
        void stdoutがバッファに書き込まれる() {
            var executor = createExecutor();
            executor.setStdout("hello", "world");
            var ts = setup(queuePrompter("echo hello"), executor);

            ts.command().execute(ts.context()).join();

            assertEquals(1, executor.executedCommands.size());
            assertEquals("echo hello", executor.executedCommands.get(0));

            var outputBuffer = ts.context().bufferManager().findByName("*Shell Command Output*");
            assertTrue(outputBuffer.isPresent());
            String text = outputBuffer.get().getText();
            assertTrue(text.contains("$ echo hello"), "コマンド行が含まれること: " + text);
            assertTrue(text.contains("hello\n"), "stdout出力が含まれること: " + text);
            assertTrue(text.contains("world\n"), "stdout出力が含まれること: " + text);
            assertTrue(text.contains("exit code: 0"), "exit codeが含まれること: " + text);
        }

        @Test
        void stderrがWARNINGフェイスで色分けされる() {
            var executor = createExecutor();
            executor.setStderr("error occurred");
            executor.setExitCode(1);
            var ts = setup(queuePrompter("bad-cmd"), executor);

            ts.command().execute(ts.context()).join();

            var outputBuffer = ts.context().bufferManager().findByName("*Shell Command Output*");
            assertTrue(outputBuffer.isPresent());
            String text = outputBuffer.get().getText();
            assertTrue(text.contains("error occurred"), "stderr出力が含まれること: " + text);
            assertTrue(text.contains("exit code: 1"), "exit codeが含まれること: " + text);

            // WARNING face が設定されていることを確認
            var buf = outputBuffer.get();
            int errorStart = text.indexOf("error occurred");
            int errorEnd = errorStart + "error occurred\n".length();
            var spans = buf.getFaceSpans(errorStart, errorEnd);
            assertFalse(spans.isEmpty(), "stderrにフェイスが設定されていること");
            assertEquals(FaceName.WARNING, spans.get(0).faceName(), "stderrにWARNINGフェイスが設定されていること");
        }

        @Test
        void 作業ディレクトリがバッファのデフォルトディレクトリになる() {
            var executor = createExecutor();
            var ts = setup(queuePrompter("pwd"), executor);

            ts.command().execute(ts.context()).join();

            assertEquals(1, executor.workingDirectories.size());
            assertEquals(Path.of("/home/user/project"), executor.workingDirectories.get(0));
        }
    }

    @Nested
    class ウィンドウ分割 {

        @Test
        void 出力バッファが下にウィンドウ分割して表示される() {
            var executor = createExecutor();
            executor.setStdout("output");
            var ts = setup(queuePrompter("echo output"), executor);

            int windowCountBefore = ts.frame().getWindowTree().windows().size();
            ts.command().execute(ts.context()).join();
            int windowCountAfter = ts.frame().getWindowTree().windows().size();

            assertEquals(windowCountBefore + 1, windowCountAfter, "ウィンドウが1つ増加すること");
        }

        @Test
        void 既存の出力ウィンドウがある場合は再利用する() {
            var executor = createExecutor();
            executor.setStdout("first");
            var ts = setup(queuePrompter("echo first", "echo second"), executor);

            ts.command().execute(ts.context()).join();
            int windowCountAfterFirst = ts.frame().getWindowTree().windows().size();

            // 2回目の実行前にアクティブウィンドウを元に戻す
            var windows = ts.frame().getWindowTree().windows();
            for (var w : windows) {
                if (!w.getBuffer().getName().equals("*Shell Command Output*")) {
                    ts.frame().setActiveWindow(w);
                    break;
                }
            }

            executor.setStdout("second");
            ts.command().execute(ts.context()).join();
            int windowCountAfterSecond = ts.frame().getWindowTree().windows().size();

            assertEquals(windowCountAfterFirst, windowCountAfterSecond, "ウィンドウ数が増加しないこと");
        }
    }

    @Nested
    class バッファフラッシュ {

        @Test
        void 再実行時にバッファがクリアされる() {
            var executor = createExecutor();
            executor.setStdout("first-output");
            var ts = setup(queuePrompter("echo first", "echo second"), executor);

            ts.command().execute(ts.context()).join();

            // 2回目の実行前にアクティブウィンドウを元に戻す
            var windows = ts.frame().getWindowTree().windows();
            for (var w : windows) {
                if (!w.getBuffer().getName().equals("*Shell Command Output*")) {
                    ts.frame().setActiveWindow(w);
                    break;
                }
            }

            executor.setStdout("second-output");
            ts.command().execute(ts.context()).join();

            var outputBuffer = ts.context().bufferManager().findByName("*Shell Command Output*");
            assertTrue(outputBuffer.isPresent());
            String text = outputBuffer.get().getText();
            assertFalse(text.contains("first-output"), "前回の出力がクリアされていること: " + text);
            assertTrue(text.contains("second-output"), "今回の出力が含まれること: " + text);
        }
    }

    @Nested
    class 排他制御 {

        @Test
        void 実行中に再度呼ばれた場合はメッセージを表示して拒否する() {
            var blockingExecutor = new StubExecutor();
            var blocker = new CompletableFuture<Integer>();
            blockingExecutor.setBlockingFuture(blocker);

            var ts = setup(queuePrompter("slow-cmd", "fast-cmd"), blockingExecutor);

            // 1回目を開始（完了しない）
            var first = ts.command().execute(ts.context());

            // 2回目を試行
            ts.command().execute(ts.context()).join();

            // 1回目のコマンドのみ実行されている
            assertEquals(1, blockingExecutor.executedCommands.size());

            // ブロッカーを解放
            blocker.complete(0);
            first.join();
        }
    }

    /**
     * テスト用のShellCommandExecutorスタブ。
     */
    private static class StubExecutor implements ShellCommandExecutor {

        final MutableList<String> executedCommands = Lists.mutable.empty();
        final MutableList<Path> workingDirectories = Lists.mutable.empty();
        private MutableList<String> stdoutLines = Lists.mutable.empty();
        private MutableList<String> stderrLines = Lists.mutable.empty();
        private int exitCode;
        private Optional<CompletableFuture<Integer>> blockingFuture = Optional.empty();

        void setStdout(String... lines) {
            this.stdoutLines = Lists.mutable.of(lines);
        }

        void setStderr(String... lines) {
            this.stderrLines = Lists.mutable.of(lines);
        }

        void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        void setBlockingFuture(CompletableFuture<Integer> future) {
            this.blockingFuture = Optional.of(future);
        }

        @Override
        public CompletableFuture<Integer> execute(
                String command, Path workingDirectory, Consumer<String> onStdoutLine, Consumer<String> onStderrLine) {
            executedCommands.add(command);
            workingDirectories.add(workingDirectory);
            if (blockingFuture.isPresent()) {
                return blockingFuture.get().thenApply(code -> {
                    for (String line : stdoutLines) {
                        onStdoutLine.accept(line);
                    }
                    for (String line : stderrLines) {
                        onStderrLine.accept(line);
                    }
                    return code;
                });
            }
            for (String line : stdoutLines) {
                onStdoutLine.accept(line);
            }
            for (String line : stderrLines) {
                onStderrLine.accept(line);
            }
            return CompletableFuture.completedFuture(exitCode);
        }
    }
}
