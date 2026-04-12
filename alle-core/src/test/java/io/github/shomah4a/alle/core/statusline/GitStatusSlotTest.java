package io.github.shomah4a.alle.core.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.statusline.GitBranchProvider.GitBranchInfo;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GitStatusSlotTest {

    private static StatusLineContext createContextWithFile(Path filePath) {
        var settingsRegistry = new SettingsRegistry();
        var buffer = new BufferFacade(new TextBuffer("test.txt", new GapTextModel(), settingsRegistry, filePath));
        var window = new Window(buffer);
        return new StatusLineContext(window, buffer);
    }

    private static StatusLineContext createContextWithoutFile() {
        var settingsRegistry = new SettingsRegistry();
        var buffer = new BufferFacade(new TextBuffer("*scratch*", new GapTextModel(), settingsRegistry));
        var window = new Window(buffer);
        return new StatusLineContext(window, buffer);
    }

    @Test
    void ブランチ名を表示する() {
        GitBranchProvider provider = path -> Optional.of(new GitBranchInfo("main", false));
        var slot = new GitStatusSlot(provider);
        var ctx = createContextWithFile(Path.of("/repo/test.txt"));

        assertEquals(" Git:main", slot.render(ctx));
    }

    @Test
    void ダーティな場合はアスタリスクを付与する() {
        GitBranchProvider provider = path -> Optional.of(new GitBranchInfo("feature/xyz", true));
        var slot = new GitStatusSlot(provider);
        var ctx = createContextWithFile(Path.of("/repo/test.txt"));

        assertEquals(" Git:feature/xyz*", slot.render(ctx));
    }

    @Test
    void git管理外の場合は空文字列を返す() {
        GitBranchProvider provider = path -> Optional.empty();
        var slot = new GitStatusSlot(provider);
        var ctx = createContextWithFile(Path.of("/tmp/test.txt"));

        assertEquals("", slot.render(ctx));
    }

    @Test
    void ファイルパスがないバッファでは空文字列を返す() {
        GitBranchProvider provider = path -> Optional.of(new GitBranchInfo("main", false));
        var slot = new GitStatusSlot(provider);
        var ctx = createContextWithoutFile();

        assertEquals("", slot.render(ctx));
    }

    @Test
    void name_はgit_statusを返す() {
        GitBranchProvider provider = path -> Optional.empty();
        var slot = new GitStatusSlot(provider);

        assertEquals("git-status", slot.name());
    }
}
