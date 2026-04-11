package io.github.shomah4a.alle.core.mode.modes.dired.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.modes.dired.DiredCustomColumn;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredBufferUpdater;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredMode;
import io.github.shomah4a.alle.core.mode.modes.dired.TreeDiredModel;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import java.time.ZoneId;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TreeDiredGitModeTest {

    private static TreeDiredGitMode createGitMode(GitStatusProvider provider) {
        return new TreeDiredGitMode(provider, new Keymap("test-git"), new CommandRegistry());
    }

    private static BufferFacade createDiredBuffer(Path rootDir) {
        var model = new TreeDiredModel(rootDir, dir -> Lists.immutable.empty());
        var mode = new TreeDiredMode(model, new Keymap("test"), ZoneId.of("UTC"), new CommandRegistry());
        var buffer = new BufferFacade(new TextBuffer("*Dired*", new GapTextModel(), new SettingsRegistry()));
        buffer.setMajorMode(mode);
        return buffer;
    }

    @Nested
    class onEnable {

        @Test
        void バッファ変数にカスタムカラムが設定される() {
            var provider = new StubGitStatusProvider(Maps.immutable.of(Path.of("/repo/a.txt"), "M"), "main");
            var gitMode = createGitMode(provider);
            var buffer = createDiredBuffer(Path.of("/repo"));

            gitMode.onEnable(buffer);

            var columnsOpt = buffer.getVariable(TreeDiredBufferUpdater.CUSTOM_COLUMNS_KEY);
            assertTrue(columnsOpt.isPresent());
            var columnsObj = columnsOpt.get();
            assertTrue(columnsObj instanceof ListIterable<?>);
            var columnList = (ListIterable<?>) columnsObj;
            assertEquals(1, columnList.size());
            var column = (DiredCustomColumn) columnList.get(0);
            assertEquals("git", column.header());
            assertEquals("M", column.renderCell(Path.of("/repo/a.txt")));
            assertEquals("", column.renderCell(Path.of("/repo/b.txt")));
        }

        @Test
        void バッファ変数にブランチ名サフィックスが設定される() {
            var provider = new StubGitStatusProvider(Maps.immutable.empty(), "feature/test");
            var gitMode = createGitMode(provider);
            var buffer = createDiredBuffer(Path.of("/repo"));

            gitMode.onEnable(buffer);

            var suffix = buffer.getVariable(TreeDiredBufferUpdater.ROOT_SUFFIX_KEY);
            assertTrue(suffix.isPresent());
            assertEquals("[feature/test]", suffix.get());
        }

        @Test
        void ブランチ名が空の場合はサフィックスが設定されない() {
            var provider = new StubGitStatusProvider(Maps.immutable.empty(), "");
            var gitMode = createGitMode(provider);
            var buffer = createDiredBuffer(Path.of("/repo"));

            gitMode.onEnable(buffer);

            var suffix = buffer.getVariable(TreeDiredBufferUpdater.ROOT_SUFFIX_KEY);
            assertTrue(suffix.isEmpty());
        }
    }

    @Nested
    class onDisable {

        @Test
        void バッファ変数がクリアされる() {
            var provider = new StubGitStatusProvider(Maps.immutable.of(Path.of("/repo/a.txt"), "M"), "main");
            var gitMode = createGitMode(provider);
            var buffer = createDiredBuffer(Path.of("/repo"));

            gitMode.onEnable(buffer);
            gitMode.onDisable(buffer);

            assertTrue(buffer.getVariable(TreeDiredBufferUpdater.CUSTOM_COLUMNS_KEY)
                    .isEmpty());
            assertTrue(
                    buffer.getVariable(TreeDiredBufferUpdater.ROOT_SUFFIX_KEY).isEmpty());
        }
    }

    private record StubGitStatusProvider(MapIterable<Path, String> statuses, String branch)
            implements GitStatusProvider {

        @Override
        public MapIterable<Path, String> getFileStatuses(Path rootDirectory) {
            return statuses;
        }

        @Override
        public String getBranch(Path rootDirectory) {
            return branch;
        }

        @Override
        public void stageFiles(Path rootDirectory, ListIterable<Path> files) {
            // no-op for test
        }

        @Override
        public boolean isTracked(Path rootDirectory, Path file) {
            return false;
        }

        @Override
        public void removeFiles(Path rootDirectory, ListIterable<Path> files, boolean force) {
            // no-op for test
        }

        @Override
        public void moveFile(Path rootDirectory, Path source, Path destination) {
            // no-op for test
        }
    }
}
