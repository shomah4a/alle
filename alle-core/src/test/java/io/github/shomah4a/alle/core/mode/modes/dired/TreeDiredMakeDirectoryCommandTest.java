package io.github.shomah4a.alle.core.mode.modes.dired;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.InputUpdateListener;
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
import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeDiredMakeDirectoryCommandTest {

    private static final FileAttributes A = FileAttributes.EMPTY;
    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final SettingsRegistry SETTINGS = new SettingsRegistry();
    private static final Path HOME = Path.of("/home/testuser");
    private static final DirectoryLister STUB_LISTER = directory -> Lists.immutable.empty();

    private static FilePathInputPrompter confirmingPrompter(String confirmingValue) {
        InputPrompter mock = new InputPrompter() {
            @Override
            public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
                return CompletableFuture.completedFuture(new PromptResult.Confirmed(confirmingValue));
            }

            @Override
            public CompletableFuture<PromptResult> prompt(
                    String message,
                    String initialValue,
                    InputHistory history,
                    Completer completer,
                    InputUpdateListener updateListener) {
                return CompletableFuture.completedFuture(new PromptResult.Confirmed(confirmingValue));
            }
        };
        return new FilePathInputPrompter(mock, STUB_LISTER, HOME);
    }

    private static FilePathInputPrompter cancellingPrompter() {
        InputPrompter mock = (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
        return new FilePathInputPrompter(mock, STUB_LISTER, HOME);
    }

    private record DiredSetup(
            CommandContext context, TreeDiredMode diredMode, Window window, MessageBuffer messageBuffer) {}

    private static DiredSetup setupDired(MutableMap<Path, ListIterable<DirectoryEntry>> entries, Path rootDir) {
        DirectoryLister lister = directory -> entries.getIfAbsentValue(directory, Lists.immutable.empty());
        var model = new TreeDiredModel(rootDir, lister);
        var keymap = new Keymap("tree-dired-test");
        var mode = new TreeDiredMode(model, keymap, UTC, new CommandRegistry());

        var bufferFacade = new BufferFacade(new TextBuffer("*Dired " + rootDir + "*", new GapTextModel(), SETTINGS));
        bufferFacade.setMajorMode(mode);
        TreeDiredRenderer.render(bufferFacade, rootDir, model.getVisibleEntries(), UTC, Lists.immutable.empty(), "");
        bufferFacade.setReadOnly(true);

        var window = new Window(bufferFacade);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
        var frame = new Frame(window, minibuffer);
        var bufferManager = new BufferManager();
        bufferManager.add(bufferFacade);

        var messageBuffer = new MessageBuffer("*Messages*", 100, SETTINGS);
        var stubBufferIO = new BufferIO(
                source -> {
                    throw new IOException("stub");
                },
                destination -> {
                    throw new IOException("stub");
                },
                SETTINGS);
        var pathOpenService = new PathOpenService(
                stubBufferIO,
                new AutoModeMap(TextMode::new),
                new ModeRegistry(),
                SETTINGS,
                path -> false,
                (pathString, bm, f) -> {});

        var prompter =
                (InputPrompter) (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());

        var context = new CommandContext(
                frame,
                bufferManager,
                window,
                prompter,
                Lists.immutable.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                messageBuffer,
                new MessageBuffer("*Warnings*", 100, SETTINGS),
                SETTINGS,
                new CommandResolver(new CommandRegistry()),
                new NoOpOverridingKeymapController(),
                pathOpenService);

        return new DiredSetup(context, mode, window, messageBuffer);
    }

    private static void moveToLine(DiredSetup setup, int lineIndex) {
        var buffer = setup.window().getBuffer();
        int offset = buffer.lineStartOffset(lineIndex);
        setup.window().setPoint(offset);
    }

    @Nested
    class ディレクトリ作成 {

        @Test
        void 指定パスでcreateDirectoriesが呼ばれる() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredMakeDirectoryCommand(ops, new InputHistory(), confirmingPrompter("/p/newdir"));

            var setup = setupDired(entries, Path.of("/p"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertEquals(1, ops.operations.size());
            assertEquals("mkdir:/p/newdir", ops.operations.get(0));
        }

        @Test
        void 成功時にメッセージが表示される() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredMakeDirectoryCommand(ops, new InputHistory(), confirmingPrompter("/p/newdir"));

            var setup = setupDired(entries, Path.of("/p"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertTrue(setup.messageBuffer().getLastMessage().orElse("").contains("ディレクトリを作成しました"));
        }
    }

    @Nested
    class キャンセル {

        @Test
        void キャンセル時はcreateDirectoriesが呼ばれない() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var ops = new StubFileOperations();
            var cmd = new TreeDiredMakeDirectoryCommand(ops, new InputHistory(), cancellingPrompter());

            var setup = setupDired(entries, Path.of("/p"));
            moveToLine(setup, 2);

            cmd.execute(setup.context()).join();

            assertTrue(ops.operations.isEmpty());
        }
    }

    @Nested
    class TreeDiredモード以外 {

        @Test
        void TreeDiredモード以外では何もしない() {
            var ops = new StubFileOperations();
            var cmd = new TreeDiredMakeDirectoryCommand(ops, new InputHistory(), confirmingPrompter("/tmp/dir"));

            var buffer = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), SETTINGS));
            var window = new Window(buffer);
            var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
            var frame = new Frame(window, minibuffer);
            var bufferManager = new BufferManager();
            bufferManager.add(buffer);

            var stubBufferIO = new BufferIO(
                    source -> {
                        throw new IOException("stub");
                    },
                    destination -> {
                        throw new IOException("stub");
                    },
                    SETTINGS);
            var pathOpenService = new PathOpenService(
                    stubBufferIO,
                    new AutoModeMap(TextMode::new),
                    new ModeRegistry(),
                    SETTINGS,
                    path -> false,
                    (pathString, bm, f) -> {});

            var prompter = (InputPrompter)
                    (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());

            var context = new CommandContext(
                    frame,
                    bufferManager,
                    window,
                    prompter,
                    Lists.immutable.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    new KillRing(),
                    new MessageBuffer("*Messages*", 100, SETTINGS),
                    new MessageBuffer("*Warnings*", 100, SETTINGS),
                    SETTINGS,
                    new CommandResolver(new CommandRegistry()),
                    new NoOpOverridingKeymapController(),
                    pathOpenService);

            cmd.execute(context).join();

            assertTrue(ops.operations.isEmpty());
        }
    }
}
