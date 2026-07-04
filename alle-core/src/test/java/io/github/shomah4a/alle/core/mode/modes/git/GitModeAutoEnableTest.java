package io.github.shomah4a.alle.core.mode.modes.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.CommandRegistry;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.ModeRegistry;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GitModeAutoEnableTest {

    private static final Path REPO_ROOT = Path.of("/repo");
    private static final Path FILE_IN_REPO = Path.of("/repo/src/A.java");

    private BufferFacade newFileBuffer(Path filePath) {
        var buf = new BufferFacade(new TextBuffer("A.java", new GapTextModel(), new SettingsRegistry()));
        buf.setFilePath(filePath);
        return buf;
    }

    private static GitRepositoryLocator locatorReturning(Optional<Path> value) {
        return new GitRepositoryLocator(Duration.ofSeconds(60), 100, path -> value);
    }

    private static boolean hasGitMode(BufferFacade buffer) {
        return buffer.getMinorModes().anySatisfy(m -> "git".equals(m.name()));
    }

    @Test
    void filePathがリポジトリ配下ならgit_modeが有効化される() {
        var modeRegistry = new ModeRegistry();
        var locator = locatorReturning(Optional.of(REPO_ROOT));
        var buffer = newFileBuffer(FILE_IN_REPO);

        GitModeInitializer.tryAutoEnable(buffer, modeRegistry, locator, new Keymap("git"), new CommandRegistry());

        assertTrue(hasGitMode(buffer));
    }

    @Test
    void filePathがリポジトリ外ならgit_modeが有効化されない() {
        var modeRegistry = new ModeRegistry();
        var locator = locatorReturning(Optional.empty());
        var buffer = newFileBuffer(FILE_IN_REPO);

        GitModeInitializer.tryAutoEnable(buffer, modeRegistry, locator, new Keymap("git"), new CommandRegistry());

        assertTrue(!hasGitMode(buffer));
    }

    @Test
    void filePath未設定バッファではgit_modeが有効化されない() {
        var modeRegistry = new ModeRegistry();
        var locator = locatorReturning(Optional.of(REPO_ROOT));
        var buffer = new BufferFacade(new TextBuffer("scratch", new GapTextModel(), new SettingsRegistry()));

        GitModeInitializer.tryAutoEnable(buffer, modeRegistry, locator, new Keymap("git"), new CommandRegistry());

        assertTrue(!hasGitMode(buffer));
    }

    @Test
    void 有効化後のgit_modeは1つだけenableされる() {
        var modeRegistry = new ModeRegistry();
        var locator = locatorReturning(Optional.of(REPO_ROOT));
        var buffer = newFileBuffer(FILE_IN_REPO);

        GitModeInitializer.tryAutoEnable(buffer, modeRegistry, locator, new Keymap("git"), new CommandRegistry());
        GitModeInitializer.tryAutoEnable(buffer, modeRegistry, locator, new Keymap("git"), new CommandRegistry());

        assertEquals(1, buffer.getMinorModes().count(m -> "git".equals(m.name())));
    }
}
