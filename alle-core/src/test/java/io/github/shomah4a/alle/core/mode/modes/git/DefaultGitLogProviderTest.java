package io.github.shomah4a.alle.core.mode.modes.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class DefaultGitLogProviderTest {

    private static final String US = String.valueOf((char) 0x1F);
    private static final String RS = String.valueOf((char) 0x1E);

    private static String entry(String hash, String iso, String author, String subject, String body) {
        return hash + US + iso + US + author + US + subject + US + body + RS;
    }

    @Test
    void 単一コミットの出力が1件のGitLogEntryにパースされる() {
        String out = entry("abc1234", "2026-07-04T12:34:56+09:00", "shoma", "fix: bug", "詳細ボディ");
        var provider = new DefaultGitLogProvider((dir, cmd) -> Optional.of(out));

        var entries = provider.getLog(Path.of("/repo"), Optional.empty(), 10);

        assertEquals(1, entries.size());
        var e = entries.get(0);
        assertEquals("abc1234", e.shortHash());
        assertEquals(Instant.parse("2026-07-04T03:34:56Z"), e.commitTime());
        assertEquals("shoma", e.author());
        assertEquals("fix: bug", e.subject());
        assertEquals("詳細ボディ", e.body());
    }

    @Test
    void 複数コミットの出力がすべてパースされる() {
        String out = entry("aaa", "2026-07-04T00:00:00Z", "a", "subj-a", "")
                + entry("bbb", "2026-07-03T00:00:00Z", "b", "subj-b", "body-b")
                + entry("ccc", "2026-07-02T00:00:00Z", "c", "subj-c", "");
        var provider = new DefaultGitLogProvider((dir, cmd) -> Optional.of(out));

        var entries = provider.getLog(Path.of("/repo"), Optional.empty(), 10);

        assertEquals(3, entries.size());
        assertEquals("aaa", entries.get(0).shortHash());
        assertEquals("bbb", entries.get(1).shortHash());
        assertEquals("ccc", entries.get(2).shortHash());
    }

    @Test
    void 空出力の場合は空リストが返る() {
        var provider = new DefaultGitLogProvider((dir, cmd) -> Optional.of(""));

        var entries = provider.getLog(Path.of("/repo"), Optional.empty(), 10);

        assertTrue(entries.isEmpty());
    }

    @Test
    void processRunnerがemptyを返した場合は空リストが返る() {
        var provider = new DefaultGitLogProvider((dir, cmd) -> Optional.empty());

        var entries = provider.getLog(Path.of("/repo"), Optional.empty(), 10);

        assertTrue(entries.isEmpty());
    }

    @Test
    void フィールド数不足の破損レコードはスキップされる() {
        String out = "onlyhash" + RS
                + entry("bbb", "2026-07-03T00:00:00Z", "b", "subj-b", "body-b");
        var provider = new DefaultGitLogProvider((dir, cmd) -> Optional.of(out));

        var entries = provider.getLog(Path.of("/repo"), Optional.empty(), 10);

        assertEquals(1, entries.size());
        assertEquals("bbb", entries.get(0).shortHash());
    }

    @Test
    void コミット時刻パース失敗のレコードはスキップされる() {
        String out = entry("aaa", "not-a-date", "a", "subj-a", "")
                + entry("bbb", "2026-07-03T00:00:00Z", "b", "subj-b", "body-b");
        var provider = new DefaultGitLogProvider((dir, cmd) -> Optional.of(out));

        var entries = provider.getLog(Path.of("/repo"), Optional.empty(), 10);

        assertEquals(1, entries.size());
        assertEquals("bbb", entries.get(0).shortHash());
    }

    @Test
    void 対象Pathなしのコマンドラインはpath指定を含まない() {
        var captured = new AtomicReference<String[]>();
        var provider = new DefaultGitLogProvider((dir, cmd) -> {
            captured.set(cmd);
            return Optional.of("");
        });

        provider.getLog(Path.of("/repo"), Optional.empty(), 50);

        var cmd = java.util.Objects.requireNonNull(captured.get());
        assertEquals("git", cmd[0]);
        assertEquals("log", cmd[1]);
        assertEquals("--max-count=50", cmd[2]);
        assertTrue(cmd[3].startsWith("--pretty=format:"));
        assertEquals(4, cmd.length);
    }

    @Test
    void 対象Pathありのコマンドラインは末尾にダブルダッシュとpathが付く() {
        var captured = new AtomicReference<String[]>();
        var provider = new DefaultGitLogProvider((dir, cmd) -> {
            captured.set(cmd);
            return Optional.of("");
        });

        provider.getLog(Path.of("/repo"), Optional.of(Path.of("/repo/src/A.java")), 20);

        var cmd = java.util.Objects.requireNonNull(captured.get());
        assertEquals(6, cmd.length);
        assertEquals("--max-count=20", cmd[2]);
        assertEquals("--", cmd[4]);
        assertEquals("/repo/src/A.java", cmd[5]);
    }

    @Test
    void 引数repoRootがprocessRunnerのworkingDirに渡される() {
        var captured = new AtomicReference<Path>();
        var provider = new DefaultGitLogProvider((dir, cmd) -> {
            captured.set(dir);
            return Optional.of("");
        });

        provider.getLog(Path.of("/some/repo"), Optional.empty(), 10);

        assertEquals(Path.of("/some/repo"), captured.get());
    }
}
