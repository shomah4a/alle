package io.github.shomah4a.alle.core.command.commands;

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
import io.github.shomah4a.alle.core.input.DirectoryLister;
import io.github.shomah4a.alle.core.input.FileOperations;
import io.github.shomah4a.alle.core.input.FilePathInputPrompter;
import io.github.shomah4a.alle.core.input.InputHistory;
import io.github.shomah4a.alle.core.input.InputPrompter;
import io.github.shomah4a.alle.core.input.InputUpdateListener;
import io.github.shomah4a.alle.core.input.PromptResult;
import io.github.shomah4a.alle.core.io.BufferIO;
import io.github.shomah4a.alle.core.io.PathOpenService;
import io.github.shomah4a.alle.core.mode.AutoModeMap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.mode.modes.text.TextMode;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MakeDirectoryCommandTest {

    private static final Path HOME = Path.of("/home/testuser");
    private static final SettingsRegistry SETTINGS = new SettingsRegistry();
    private static final DirectoryLister STUB_LISTER = directory -> Lists.immutable.empty();

    private Frame frame;
    private BufferManager bufferManager;
    private MessageBuffer messageBuffer;

    @BeforeEach
    void setUp() {
        var buffer = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), SETTINGS));
        var window = new Window(buffer);
        var minibuffer = new Window(new BufferFacade(new TextBuffer("*Minibuffer*", new GapTextModel(), SETTINGS)));
        frame = new Frame(window, minibuffer);
        bufferManager = new BufferManager();
        bufferManager.add(buffer);
        messageBuffer = new MessageBuffer("*Messages*", 100, SETTINGS);
    }

    private FilePathInputPrompter confirmingPrompter(String confirmingValue) {
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

    private FilePathInputPrompter cancellingPrompter() {
        InputPrompter mock = (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled());
        return new FilePathInputPrompter(mock, STUB_LISTER, HOME);
    }

    private CommandContext createContext() {
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

        return new CommandContext(
                frame,
                bufferManager,
                frame.getActiveWindow(),
                (message, history) -> CompletableFuture.completedFuture(new PromptResult.Cancelled()),
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
    }

    @Nested
    class ディレクトリ作成 {

        @Test
        void 指定パスでcreateDirectoriesが呼ばれる() {
            var ops = new RecordingFileOperations();
            var cmd = new MakeDirectoryCommand(
                    ops, Path.of("/test"), new InputHistory(), confirmingPrompter("/tmp/newdir"));
            var context = createContext();

            cmd.execute(context).join();

            assertEquals(1, ops.createdDirectories.size());
            assertEquals(Path.of("/tmp/newdir").toAbsolutePath().normalize(), ops.createdDirectories.get(0));
        }

        @Test
        void 成功時にメッセージが表示される() {
            var ops = new RecordingFileOperations();
            var cmd = new MakeDirectoryCommand(
                    ops, Path.of("/test"), new InputHistory(), confirmingPrompter("/tmp/newdir"));
            var context = createContext();

            cmd.execute(context).join();

            assertTrue(messageBuffer.getLastMessage().orElse("").contains("ディレクトリを作成しました"));
        }
    }

    @Nested
    class キャンセル {

        @Test
        void キャンセル時はcreateDirectoriesが呼ばれない() {
            var ops = new RecordingFileOperations();
            var cmd = new MakeDirectoryCommand(ops, Path.of("/test"), new InputHistory(), cancellingPrompter());
            var context = createContext();

            cmd.execute(context).join();

            assertTrue(ops.createdDirectories.isEmpty());
        }
    }

    @Nested
    class エラー時 {

        @Test
        void IOException発生時にエラーメッセージが表示される() {
            var ops = new FailingFileOperations();
            var cmd = new MakeDirectoryCommand(
                    ops, Path.of("/test"), new InputHistory(), confirmingPrompter("/tmp/fail"));
            var context = createContext();

            cmd.execute(context).join();

            assertTrue(messageBuffer.getLastMessage().orElse("").contains("ディレクトリ作成に失敗"));
        }
    }

    private static class RecordingFileOperations implements FileOperations {
        final MutableList<Path> createdDirectories = Lists.mutable.empty();

        @Override
        public void copy(Path source, Path target) throws IOException {}

        @Override
        public void move(Path source, Path target) throws IOException {}

        @Override
        public void delete(Path path) throws IOException {}

        @Override
        public void setOwner(Path path, String owner) throws IOException {}

        @Override
        public void setPermissions(Path path, String permissions) throws IOException {}

        @Override
        public void createDirectories(Path path) throws IOException {
            createdDirectories.add(path);
        }
    }

    private static class FailingFileOperations implements FileOperations {
        @Override
        public void copy(Path source, Path target) throws IOException {}

        @Override
        public void move(Path source, Path target) throws IOException {}

        @Override
        public void delete(Path path) throws IOException {}

        @Override
        public void setOwner(Path path, String owner) throws IOException {}

        @Override
        public void setPermissions(Path path, String permissions) throws IOException {}

        @Override
        public void createDirectories(Path path) throws IOException {
            throw new IOException("Permission denied");
        }
    }
}
