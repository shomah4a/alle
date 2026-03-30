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
import io.github.shomah4a.alle.core.command.KillRing;
import io.github.shomah4a.alle.core.input.DirectoryEntry;
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FileAttributes;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.keybind.Keymap;
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

class TreeDiredFileCommandTest {

    private static final FileAttributes A = FileAttributes.EMPTY;
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static DirectoryLister stubLister(MutableMap<Path, ListIterable<DirectoryEntry>> entries) {
        return directory -> entries.getIfAbsentValue(directory, Lists.immutable.empty());
    }

    /**
     * ミニバッファ入力を順番に返すプロンプター。
     */
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
                    String message,
                    String initialValue,
                    InputHistory history,
                    io.github.shomah4a.alle.core.input.Completer completer) {
                return prompt(message, history);
            }
        };
    }

    /**
     * Tree Dired バッファとコンテキストを構成する。
     */
    private record DiredSetup(CommandContext context, TreeDiredMode diredMode, Window window) {}

    private static DiredSetup setupDired(
            MutableMap<Path, ListIterable<DirectoryEntry>> entries, Path rootDir, InputPrompter prompter) {
        var lister = stubLister(entries);
        var model = new TreeDiredModel(rootDir, lister);
        var keymap = new Keymap("tree-dired-test");
        var mode = new TreeDiredMode(model, keymap, UTC);

        var settings = new SettingsRegistry();
        var bufferFacade = new BufferFacade(new TextBuffer("*Dired " + rootDir + "*", new GapTextModel(), settings));
        bufferFacade.setMajorMode(mode);
        TreeDiredRenderer.render(bufferFacade, rootDir, model.getVisibleEntries(), UTC);
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
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, settings),
                new MessageBuffer("*Warnings*", 100, settings),
                settings,
                new io.github.shomah4a.alle.core.command.CommandResolver(registry));

        return new DiredSetup(context, mode, window);
    }

    /**
     * カーソルを指定行に移動する（0-indexed の行番号）。
     */
    private static void moveToLine(DiredSetup setup, int lineIndex) {
        var buffer = setup.window().getBuffer();
        int offset = buffer.lineStartOffset(lineIndex);
        setup.window().setPoint(offset);
    }

    @Nested
    class コピーコマンド {

        @Test
        void 単一ファイルをコピーする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredCopyCommand(ops, new InputHistory(), new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("/p/b.txt"));
            moveToLine(setup, 2); // 最初のエントリ行

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("copy:/p/a.txt->/p/b.txt", ops.operations.get(0));
        }

        @Test
        void マーク済みエントリを対象にコピーする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredCopyCommand(ops, new InputHistory(), new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("/dest/"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));
            setup.diredMode().getModel().mark(Path.of("/p/b.txt"));

            cmd.execute(setup.context()).join();

            assertEquals(2, ops.operations.size());
            assertTrue(ops.operations.contains("copy:/p/a.txt->/dest/a.txt"));
            assertTrue(ops.operations.contains("copy:/p/b.txt->/dest/b.txt"));
        }

        @Test
        void ディレクトリを含む場合に確認でnoならコピーしない() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.Directory(Path.of("/p/sub"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredCopyCommand(ops, new InputHistory(), new InputHistory());

            // 1つ目: コピー先パス, 2つ目: 再帰確認で "n"
            var setup = setupDired(entries, Path.of("/p"), queuePrompter("/dest/sub", "n"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertTrue(ops.operations.isEmpty());
        }

        @Test
        void ディレクトリを含む場合に確認でyesなら再帰コピーする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.Directory(Path.of("/p/sub"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredCopyCommand(ops, new InputHistory(), new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("/dest/sub", "y"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("copy:/p/sub->/dest/sub", ops.operations.get(0));
        }
    }

    @Nested
    class 削除コマンド {

        @Test
        void 単一ファイルをyesで削除する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredDeleteCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("y"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("delete:/p/a.txt", ops.operations.get(0));
        }

        @Test
        void 単一ファイルをnoでキャンセルする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredDeleteCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("n"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertTrue(ops.operations.isEmpty());
        }

        @Test
        void ディレクトリを含む場合にrecursiveで再帰削除する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.Directory(Path.of("/p/sub"), A),
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredDeleteCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("r"));
            setup.diredMode().getModel().mark(Path.of("/p/sub"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));

            cmd.execute(setup.context()).join();

            assertEquals(2, ops.operations.size());
            assertTrue(ops.operations.contains("delete:/p/sub"));
            assertTrue(ops.operations.contains("delete:/p/a.txt"));
        }

        @Test
        void ディレクトリを含む場合にfilesOnlyでファイルのみ削除する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.Directory(Path.of("/p/sub"), A),
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredDeleteCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("f"));
            setup.diredMode().getModel().mark(Path.of("/p/sub"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("delete:/p/a.txt", ops.operations.get(0));
        }
    }

    @Nested
    class リネームコマンド {

        @Test
        void 単一ファイルをリネームする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredRenameCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("/p/b.txt"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("move:/p/a.txt->/p/b.txt", ops.operations.get(0));
        }
    }

    @Nested
    class chmodコマンド {

        @Test
        void パーミッションを変更する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredChmodCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("rwxr-xr-x"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("chmod:/p/a.txt:rwxr-xr-x", ops.operations.get(0));
        }
    }

    @Nested
    class chownコマンド {

        @Test
        void オーナーを変更する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredChownCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("newowner"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("chown:/p/a.txt:newowner", ops.operations.get(0));
        }
    }

    @Nested
    class マーク操作 {

        @Test
        void マーク済みエントリがあればそれを対象にする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredChmodCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("rwxrwxrwx"));
            // b.txt だけマーク、カーソルは a.txt 行
            setup.diredMode().getModel().mark(Path.of("/p/b.txt"));
            moveToLine(setup, 2); // a.txt行

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("chmod:/p/b.txt:rwxrwxrwx", ops.operations.get(0));
        }

        @Test
        void 操作成功後にマークがクリアされる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredChownCommand(ops, new InputHistory());

            var setup = setupDired(entries, Path.of("/p"), queuePrompter("user"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));

            cmd.execute(setup.context()).join();

            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
            assertTrue(setup.diredMode().getModel().getMarkedPaths().isEmpty());
        }
    }
}
