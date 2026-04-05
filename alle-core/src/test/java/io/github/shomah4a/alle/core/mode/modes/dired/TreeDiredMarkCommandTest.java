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
import io.github.shomah4a.alle.core.command.commands.NextLineCommand;
import io.github.shomah4a.alle.core.input.Completer;
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
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeDiredMarkCommandTest {

    private static final FileAttributes A = FileAttributes.EMPTY;
    private static final ZoneId UTC = ZoneId.of("UTC");

    private static DirectoryLister stubLister(MutableMap<Path, ListIterable<DirectoryEntry>> entries) {
        return directory -> entries.getIfAbsentValue(directory, Lists.immutable.empty());
    }

    private static InputPrompter noOpPrompter() {
        return new InputPrompter() {
            @Override
            public CompletableFuture<PromptResult> prompt(String message, InputHistory history) {
                return CompletableFuture.completedFuture(new PromptResult.Cancelled());
            }

            @Override
            public CompletableFuture<PromptResult> prompt(
                    String message, String initialValue, InputHistory history, Completer completer) {
                return prompt(message, history);
            }
        };
    }

    private record DiredSetup(CommandContext context, TreeDiredMode diredMode, Window window) {}

    private static DiredSetup setupDired(MutableMap<Path, ListIterable<DirectoryEntry>> entries, Path rootDir) {
        var lister = stubLister(entries);
        var model = new TreeDiredModel(rootDir, lister);
        var keymap = new Keymap("tree-dired-test");
        var modeRegistry = new CommandRegistry();
        var mode = new TreeDiredMode(model, keymap, UTC, modeRegistry);

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
        registry.register(new NextLineCommand());
        var context = new CommandContext(
                frame,
                bufferManager,
                window,
                noOpPrompter(),
                Lists.immutable.empty(),
                Optional.empty(),
                Optional.empty(),
                new KillRing(),
                new MessageBuffer("*Messages*", 100, settings),
                new MessageBuffer("*Warnings*", 100, settings),
                settings,
                new CommandResolver(registry),
                new NoOpOverridingKeymapController());

        return new DiredSetup(context, mode, window);
    }

    private static void moveToLine(DiredSetup setup, int lineIndex) {
        var buffer = setup.window().getBuffer();
        int offset = buffer.lineStartOffset(lineIndex);
        setup.window().setPoint(offset);
    }

    private static int currentLineIndex(DiredSetup setup) {
        var buffer = setup.window().getBuffer();
        return buffer.lineIndexForOffset(setup.window().getPoint());
    }

    @Nested
    class markコマンド {

        @Test
        void 単一行をマークして次行に移動する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));
            moveToLine(setup, 2); // a.txt行

            new TreeDiredMarkCommand().execute(setup.context()).join();

            assertTrue(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
            assertEquals(3, currentLineIndex(setup));
        }

        @Test
        void リージョン内のエントリを一括マークする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/c.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));

            var buffer = setup.window().getBuffer();
            int startOffset = buffer.lineStartOffset(2); // a.txt行
            int endOffset = buffer.lineStartOffset(3); // b.txt行
            setup.window().setMark(startOffset);
            setup.window().setPoint(endOffset);

            new TreeDiredMarkCommand().execute(setup.context()).join();

            assertTrue(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
            assertTrue(setup.diredMode().getModel().isMarked(Path.of("/p/b.txt")));
            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/c.txt")));
            assertTrue(setup.window().getRegionStart().isEmpty());
        }

        @Test
        void ヘッダ行ではマークせず何もしない() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));
            moveToLine(setup, 0);

            new TreeDiredMarkCommand().execute(setup.context()).join();

            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
        }
    }

    @Nested
    class unmarkコマンド {

        @Test
        void 単一行をアンマークして次行に移動する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));
            moveToLine(setup, 2);

            new TreeDiredUnmarkCommand().execute(setup.context()).join();

            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
            assertEquals(3, currentLineIndex(setup));
        }

        @Test
        void リージョン内のエントリを一括アンマークする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/c.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));
            setup.diredMode().getModel().mark(Path.of("/p/b.txt"));
            setup.diredMode().getModel().mark(Path.of("/p/c.txt"));

            var buffer = setup.window().getBuffer();
            int startOffset = buffer.lineStartOffset(2);
            int endOffset = buffer.lineStartOffset(3);
            setup.window().setMark(startOffset);
            setup.window().setPoint(endOffset);

            new TreeDiredUnmarkCommand().execute(setup.context()).join();

            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/b.txt")));
            assertTrue(setup.diredMode().getModel().isMarked(Path.of("/p/c.txt")));
            assertTrue(setup.window().getRegionStart().isEmpty());
        }

        @Test
        void ヘッダ行ではアンマークせず何もしない() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));
            moveToLine(setup, 0);

            new TreeDiredUnmarkCommand().execute(setup.context()).join();

            assertTrue(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
        }
    }

    @Nested
    class toggleMarkコマンド {

        @Test
        void 単一行をトグルして次行に移動する() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));
            moveToLine(setup, 2);

            new TreeDiredToggleMarkCommand().execute(setup.context()).join();

            assertTrue(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
            assertEquals(3, currentLineIndex(setup));
        }

        @Test
        void リージョン内のエントリを一括トグルする() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(
                    Path.of("/p"),
                    Lists.immutable.of(
                            new DirectoryEntry.File(Path.of("/p/a.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/b.txt"), A),
                            new DirectoryEntry.File(Path.of("/p/c.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));
            setup.diredMode().getModel().mark(Path.of("/p/a.txt"));

            var buffer = setup.window().getBuffer();
            int startOffset = buffer.lineStartOffset(2);
            int endOffset = buffer.lineStartOffset(3);
            setup.window().setMark(startOffset);
            setup.window().setPoint(endOffset);

            new TreeDiredToggleMarkCommand().execute(setup.context()).join();

            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
            assertTrue(setup.diredMode().getModel().isMarked(Path.of("/p/b.txt")));
            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/c.txt")));
            assertTrue(setup.window().getRegionStart().isEmpty());
        }

        @Test
        void ヘッダ行ではトグルせず何もしない() {
            var entries = Maps.mutable.<Path, ListIterable<DirectoryEntry>>empty();
            entries.put(Path.of("/p"), Lists.immutable.of(new DirectoryEntry.File(Path.of("/p/a.txt"), A)));
            var setup = setupDired(entries, Path.of("/p"));
            moveToLine(setup, 0);

            new TreeDiredToggleMarkCommand().execute(setup.context()).join();

            assertFalse(setup.diredMode().getModel().isMarked(Path.of("/p/a.txt")));
        }
    }
}
