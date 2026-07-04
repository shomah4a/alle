package io.github.shomah4a.alle.core.mode.modes.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.time.Instant;
import java.time.ZoneId;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class GitLogRendererTest {

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static GitLogEntry entry(String hash, String isoInstant, String author, String subject, String body) {
        return new GitLogEntry(hash, Instant.parse(isoInstant), author, subject, body);
    }

    @Test
    void 単一コミットは_commit_Author_Date_の順で組み立てられる() {
        var entries = Lists.immutable.of(entry("abc1234", "2026-07-04T10:00:00Z", "shoma", "fix: bug", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);

        String text = GitLogRenderer.buildText(entries, config);

        assertEquals(
                """
                commit abc1234
                Author: shoma
                Date:   2026-07-04

                    fix: bug
                """,
                text);
    }

    @Test
    void 複数コミットは区切り線で連結される() {
        var entries = Lists.immutable.of(
                entry("aaa", "2026-07-04T10:00:00Z", "a", "subj-a", ""),
                entry("bbb", "2026-07-03T10:00:00Z", "b", "subj-b", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);

        String text = GitLogRenderer.buildText(entries, config);

        assertTrue(text.contains("\n----\n"));
        assertTrue(text.indexOf("commit aaa") < text.indexOf("commit bbb"));
    }

    @Test
    void 幅を超えるsubjectは末尾を省略記号で切る() {
        var longSubject = "a".repeat(100);
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", longSubject, ""));
        var config = new GitLogRenderer.RenderConfig(10, 3, UTC);

        String text = GitLogRenderer.buildText(entries, config);

        assertTrue(text.contains("    " + "a".repeat(9) + "…\n"));
    }

    @Test
    void 幅以内のsubjectは切り詰められない() {
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "short", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);

        String text = GitLogRenderer.buildText(entries, config);

        assertTrue(text.contains("    short\n"));
    }

    @Test
    void 空bodyは表示に含まれない() {
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "subj", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);

        String text = GitLogRenderer.buildText(entries, config);

        long bodyIndentLines = text.lines().filter(l -> l.startsWith("    ")).count();
        assertEquals(1, bodyIndentLines);
    }

    @Test
    void 上限を超えるbodyは末尾の1行を省略記号にする() {
        String body = "line1\nline2\nline3\nline4\nline5";
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "subj", body));
        var config = new GitLogRenderer.RenderConfig(80, 2, UTC);

        String text = GitLogRenderer.buildText(entries, config);

        assertTrue(text.contains("    line1\n"));
        assertTrue(text.contains("    line2\n"));
        assertTrue(text.contains("    …\n"));
        assertTrue(!text.contains("    line3\n"));
    }

    @Test
    void 上限以内のbodyはすべて表示される() {
        String body = "line1\nline2";
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "subj", body));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);

        String text = GitLogRenderer.buildText(entries, config);

        assertTrue(text.contains("    line1\n"));
        assertTrue(text.contains("    line2\n"));
        assertTrue(!text.contains("…"));
    }

    @Test
    void 空リストの入力は空文字を返す() {
        var config = GitLogRenderer.RenderConfig.defaults();

        String text = GitLogRenderer.buildText(Lists.immutable.empty(), config);

        assertTrue(text.isEmpty());
    }

    @Test
    void Dateは指定タイムゾーンで表示される() {
        var entries = Lists.immutable.of(entry("abc", "2026-07-03T23:00:00Z", "shoma", "subj", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, ZoneId.of("Asia/Tokyo"));

        String text = GitLogRenderer.buildText(entries, config);

        assertTrue(text.contains("Date:   2026-07-04"));
    }

    private static BufferFacade newBuffer() {
        return new BufferFacade(new TextBuffer("test", new GapTextModel(), new SettingsRegistry()));
    }

    @Test
    void render_で空バッファに書き込むとbuildTextと同じ内容になる() {
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "subj", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);
        var buffer = newBuffer();

        GitLogRenderer.render(buffer, entries, config);

        String expected = GitLogRenderer.buildText(entries, config);
        assertEquals(expected, buffer.getText(0, buffer.length()));
    }

    @Test
    void appendEntries_で空バッファに追加すると先頭区切りは付かない() {
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "subj", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);
        var buffer = newBuffer();

        GitLogRenderer.appendEntries(buffer, entries, config);

        String text = buffer.getText(0, buffer.length());
        assertTrue(text.startsWith("commit abc"));
    }

    @Test
    void appendEntries_で既存バッファに追加すると先頭区切りが入る() {
        var initialEntries =
                Lists.immutable.of(entry("aaa", "2026-07-04T10:00:00Z", "shoma", "first", ""));
        var addedEntries =
                Lists.immutable.of(entry("bbb", "2026-07-03T10:00:00Z", "shoma", "second", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);
        var buffer = newBuffer();
        GitLogRenderer.render(buffer, initialEntries, config);

        GitLogRenderer.appendEntries(buffer, addedEntries, config);

        String text = buffer.getText(0, buffer.length());
        int firstIdx = text.indexOf("commit aaa");
        int sepIdx = text.indexOf("\n----\n");
        int secondIdx = text.indexOf("commit bbb");
        assertTrue(firstIdx >= 0);
        assertTrue(sepIdx > firstIdx);
        assertTrue(secondIdx > sepIdx);
    }

    @Test
    void appendEntries_で空リストを渡すとバッファは変わらない() {
        var initial =
                Lists.immutable.of(entry("aaa", "2026-07-04T10:00:00Z", "shoma", "first", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);
        var buffer = newBuffer();
        GitLogRenderer.render(buffer, initial, config);
        int before = buffer.length();

        GitLogRenderer.appendEntries(buffer, Lists.immutable.empty(), config);

        assertEquals(before, buffer.length());
    }

    @Test
    void render_後commit行にHEADING_faceが適用される() {
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "subj", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);
        var buffer = newBuffer();

        GitLogRenderer.render(buffer, entries, config);

        var spans = buffer.getFaceSpans(0, buffer.length());
        assertTrue(spans.anySatisfy(s -> s.faceName() == FaceName.HEADING));
    }

    @Test
    void render_後Author行のラベル部にKEYWORD_faceが適用される() {
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "subj", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);
        var buffer = newBuffer();

        GitLogRenderer.render(buffer, entries, config);

        String text = buffer.getText(0, buffer.length());
        int authorLabelStart = text.indexOf("Author:");
        var spans = buffer.getFaceSpans(0, buffer.length());
        assertTrue(spans.anySatisfy(
                s -> s.faceName() == FaceName.KEYWORD && s.start() == authorLabelStart && s.end() == authorLabelStart + "Author:".length()));
    }

    @Test
    void render_後Date行のラベル部にKEYWORD_faceが適用される() {
        var entries = Lists.immutable.of(entry("abc", "2026-07-04T10:00:00Z", "shoma", "subj", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);
        var buffer = newBuffer();

        GitLogRenderer.render(buffer, entries, config);

        String text = buffer.getText(0, buffer.length());
        int dateLabelStart = text.indexOf("Date:");
        var spans = buffer.getFaceSpans(0, buffer.length());
        assertTrue(spans.anySatisfy(
                s -> s.faceName() == FaceName.KEYWORD && s.start() == dateLabelStart && s.end() == dateLabelStart + "Date:".length()));
    }

    @Test
    void render_後区切り線にCOMMENT_faceが適用される() {
        var entries = Lists.immutable.of(
                entry("aaa", "2026-07-04T10:00:00Z", "a", "subj-a", ""),
                entry("bbb", "2026-07-03T10:00:00Z", "b", "subj-b", ""));
        var config = new GitLogRenderer.RenderConfig(80, 3, UTC);
        var buffer = newBuffer();

        GitLogRenderer.render(buffer, entries, config);

        var spans = buffer.getFaceSpans(0, buffer.length());
        assertTrue(spans.anySatisfy(s -> s.faceName() == FaceName.COMMENT));
    }
}
