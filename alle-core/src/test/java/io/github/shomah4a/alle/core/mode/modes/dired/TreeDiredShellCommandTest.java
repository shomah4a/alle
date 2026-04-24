package io.github.shomah4a.alle.core.mode.modes.dired;

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
import io.github.shomah4a.alle.core.input.DirectoryEntry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FileAttributes;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.io.PathOpenService;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeDiredShellCommandTest {

    private static final FileAttributes A = FileAttributes.EMPTY;
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static DirectoryLister stubLister(MutableMap<Path, ListIterable<DirectoryEntry>> entries) {
        return directory -> entries.getIfAbsentValue(directory, Lists.immutable.empty());
    }

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

    private record DiredSetup(CommandContext context, TreeDiredMode diredMode, Window window) {}

    private static DiredSetup setupDired(
            MutableMap<Path, ListIterable<DirectoryEntry>> entries, Path rootDir, InputPrompter prompter) {
        var lister = stubLister(entries);
        var model = new TreeDiredModel(rootDir, lister);
        var keymap = new Keymap("tree-dired-test");
        var mode = new TreeDiredMode(model, keymap, UTC, new CommandRegistry());

        var settings = new SettingsRegistry();
        var bufferFacade = new BufferFacade(new TextBuffer("*Dired " + rootDir + "*", new GapTextModel(), settings));
        bufferFacade.setMajorMode(mode);
        TreeDiredRenderer.render(
                bufferFacade,
                rootDir,
                model.getVisibleEntries(),
                UTC,
                org.eclipse.collections.api.factory.Lists.immutable.empty(),
                "");
        bufferFacade.setReadOnly(true);

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

        return new DiredSetup(context, mode, window);
    }

    private static void moveToLine(DiredSetup setup, int lineIndex) {
        var buffer = setup.window().getBuffer();
        int offset = buffer.lineStartOffset(lineIndex);
        setup.window().setPoint(offset);
    }

    @Nested
    class シェルクォート {

        @Test
        void 通常のパスがシングルクォートで囲まれる() {
            assertEquals("'/p/a.txt'", TreeDiredShellCommand.shellQuote("/p/a.txt"));
        }

        @Test
        void シングルクォートを含むパスがエスケープされる() {
            assertEquals("'/p/it'\\''s.txt'", TreeDiredShellCommand.shellQuote("/p/it's.txt"));
        }

        @Test
        void スペースを含むパスが正しくクォートされる() {
            assertEquals("'/p/my file.txt'", TreeDiredShellCommand.shellQuote("/p/my file.txt"));
        }
    }

    @Nested
    class 置換なしパターン {

        @Test
        void コマンド末尾にクォート済みファイルパスが追加される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("wc -l"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertEquals(1, executor.executedCommands.size());
            assertEquals("wc -l '/p/a.txt'", executor.executedCommands.get(0));
            assertEquals(Path.of("/p"), executor.workingDirectories.get(0));
        }

        @Test
        void 複数ファイルのクォート済みパスがスペース区切りで追加される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("wc -l"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));
            setup.diredMode().getModel().mark(Path.of("/p/b.txt"));

            cmd.execute(setup.context()).join();

            assertEquals(1, executor.executedCommands.size());
            assertEquals("wc -l '/p/a.txt' '/p/b.txt'", executor.executedCommands.get(0));
        }
    }

    @Nested
    class アスタリスク置換パターン {

        @Test
        void アスタリスクがクォート済みファイルパスに置換される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("ls -la *"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertEquals(1, executor.executedCommands.size());
            assertEquals("ls -la '/p/a.txt'", executor.executedCommands.get(0));
        }

        @Test
        void 複数ファイルの場合アスタリスクがクォート済みスペース区切りリストに置換される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("tar czf archive.tar.gz *"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));
            setup.diredMode().getModel().mark(Path.of("/p/b.txt"));

            cmd.execute(setup.context()).join();

            assertEquals(1, executor.executedCommands.size());
            assertEquals("tar czf archive.tar.gz '/p/a.txt' '/p/b.txt'", executor.executedCommands.get(0));
        }
    }

    @Nested
    class クエスチョン置換パターン {

        @Test
        void ファイルごとにクォート済みパスでコマンドが個別実行される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("gzip ?"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));
            setup.diredMode().getModel().mark(Path.of("/p/b.txt"));

            cmd.execute(setup.context()).join();

            assertEquals(2, executor.executedCommands.size());
            assertEquals("gzip '/p/a.txt'", executor.executedCommands.get(0));
            assertEquals("gzip '/p/b.txt'", executor.executedCommands.get(1));
        }
    }

    @Nested
    class エラーケース {

        @Test
        void アスタリスクとクエスチョンの同時使用はエラーになる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("cmd * ?"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertTrue(executor.executedCommands.isEmpty());
        }

        @Test
        void キャンセルした場合は実行されない() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter());
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertTrue(executor.executedCommands.isEmpty());
        }

        @Test
        void 空コマンドの場合は実行されない() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("  "));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertTrue(executor.executedCommands.isEmpty());
        }
    }

    @Nested
    class 結果表示 {

        @Test
        void stdout出力がバッファにストリーミングされる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var executor = new StubShellCommandExecutor();
            executor.setStdout("hello", "world");
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("echo hello"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            String activeBufferName = setup.window().getBuffer().getName();
            assertEquals("*Shell Command Output*", activeBufferName);
            String text = setup.window().getBuffer().getText();
            assertTrue(text.contains("$ echo hello '/p/a.txt'"), "コマンド行が含まれること: " + text);
            assertTrue(text.contains("hello\n"), "stdout出力が含まれること: " + text);
            assertTrue(text.contains("world\n"), "stdout出力が含まれること: " + text);
            assertTrue(text.contains("exit code: 0"), "exit codeが含まれること: " + text);
        }

        @Test
        void stderr出力がバッファに含まれる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var executor = new StubShellCommandExecutor();
            executor.setStderr("error occurred");
            executor.setExitCode(1);
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("cat"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            String activeBufferName = setup.window().getBuffer().getName();
            assertEquals("*Shell Command Output*", activeBufferName);
            String text = setup.window().getBuffer().getText();
            assertTrue(text.contains("error occurred"), "stderr出力が含まれること: " + text);
            assertTrue(text.contains("exit code: 1"), "exit codeが含まれること: " + text);
        }
    }

    @Nested
    class マーク解除 {

        @Test
        void 実行後にマークがクリアされる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var executor = new StubShellCommandExecutor();
            var cmd = new TreeDiredShellCommand(executor, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("ls"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));

            cmd.execute(setup.context()).join();

            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
            assertTrue(setup.diredMode().getModel().getMarkedPaths().isEmpty());
        }
    }
}
