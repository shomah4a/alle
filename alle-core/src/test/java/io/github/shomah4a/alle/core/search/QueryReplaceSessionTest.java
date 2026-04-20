package io.github.shomah4a.alle.core.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.command.OverridingKeymapController;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class QueryReplaceSessionTest {

    private static final SettingsRegistry SETTINGS = new SettingsRegistry();

    private static class TestOverridingKeymapController implements OverridingKeymapController {
        @Nullable
        Keymap currentKeymap;

        boolean cleared;

        @Override
        public void set(Keymap keymap, Runnable onUnboundKeyExit) {
            this.currentKeymap = keymap;
            this.cleared = false;
        }

        @Override
        public void clear() {
            this.currentKeymap = null;
            this.cleared = true;
        }
    }

    private Window windowOf(String text) {
        var model = new GapTextModel();
        if (!text.isEmpty()) {
            model.insert(0, text);
        }
        var buffer = new BufferFacade(new TextBuffer("test", model, SETTINGS));
        return new Window(buffer);
    }

    private QueryReplaceSession literal(
            Window window, MessageBuffer msg, TestOverridingKeymapController controller, String from, String to) {
        return QueryReplaceSession.forLiteral(
                window,
                msg,
                controller,
                from,
                to,
                window.getPoint(),
                window.getBuffer().length(),
                false);
    }

    private QueryReplaceSession literalInRegion(
            Window window,
            MessageBuffer msg,
            TestOverridingKeymapController controller,
            String from,
            String to,
            int rangeStart,
            int rangeEnd) {
        return QueryReplaceSession.forLiteral(window, msg, controller, from, to, rangeStart, rangeEnd, true);
    }

    private QueryReplaceSession regexp(
            Window window, MessageBuffer msg, TestOverridingKeymapController controller, String pattern, String to) {
        return QueryReplaceSession.forRegexp(
                window,
                msg,
                controller,
                Pattern.compile(pattern),
                to,
                window.getPoint(),
                window.getBuffer().length(),
                false);
    }

    private MessageBuffer messageBuffer;
    private TestOverridingKeymapController controller;

    @BeforeEach
    void setUp() {
        messageBuffer = new MessageBuffer("*Messages*", 100, SETTINGS);
        controller = new TestOverridingKeymapController();
    }

    @Nested
    class セッション開始 {

        @Test
        void マッチがない場合は即終了しoverridingKeymapは設定されない() {
            var window = windowOf("hello world");
            var session = literal(window, messageBuffer, controller, "xyz", "abc");

            session.start();

            assertTrue(session.isFinished());
            assertNull(controller.currentKeymap);
            assertTrue(messageBuffer.getLastMessage().orElse("").startsWith("Replaced 0"));
        }

        @Test
        void マッチがある場合はoverridingKeymapが設定される() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "baz");

            session.start();

            assertFalse(session.isFinished());
            assertNotNull(controller.currentKeymap);
            assertNotNull(session.getCurrentMatch());
            assertEquals(0, session.getCurrentMatch().start());
            assertEquals(3, session.getCurrentMatch().end());
        }

        @Test
        void マッチ位置にQUERY_REPLACE_MATCHフェイスが付与される() {
            var window = windowOf("foo bar");
            var session = literal(window, messageBuffer, controller, "foo", "baz");

            session.start();

            var spans = window.getBuffer().getFaceSpans(0, 7);
            assertTrue(spans.anySatisfy(
                    s -> s.faceName().equals(io.github.shomah4a.alle.core.styling.FaceName.QUERY_REPLACE_MATCH)));
        }
    }

    @Nested
    class yで置換 {

        @Test
        void 現在のマッチが置換される() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "baz");
            session.start();

            session.replaceCurrent();

            assertEquals("baz bar foo", window.getBuffer().getText());
            assertEquals(1, session.getReplacedCount());
        }

        @Test
        void 置換後に次のマッチへ移動する() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "baz");
            session.start();

            session.replaceCurrent();

            assertNotNull(session.getCurrentMatch());
            // "baz bar foo" の2つめ foo は 8..11
            assertEquals(8, session.getCurrentMatch().start());
            assertEquals(11, session.getCurrentMatch().end());
        }

        @Test
        void 全てのマッチを置換後セッションが終了する() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "baz");
            session.start();

            session.replaceCurrent();
            session.replaceCurrent();

            assertTrue(session.isFinished());
            assertEquals("baz bar baz", window.getBuffer().getText());
            assertEquals(2, session.getReplacedCount());
            assertTrue(messageBuffer.getLastMessage().orElse("").contains("Replaced 2 occurrences"));
        }

        @Test
        void 外側withTransaction経由なら1undo単位で取り消せる() {
            // CommandLoop が command.execute を withTransaction で包むのを模倣する
            var window = windowOf("foo bar foo");
            var buffer = window.getBuffer();
            var session = literal(window, messageBuffer, controller, "foo", "baz");
            session.start();

            buffer.getUndoManager().withTransaction(session::replaceCurrent);
            assertEquals("baz bar foo", buffer.getText());

            var inverse = buffer.getUndoManager().undo().orElseThrow();
            buffer.apply(inverse);

            assertEquals("foo bar foo", buffer.getText());
        }
    }

    @Nested
    class nでスキップ {

        @Test
        void 置換せずに次のマッチへ移動する() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "baz");
            session.start();

            session.skipCurrent();

            assertEquals("foo bar foo", window.getBuffer().getText());
            assertEquals(0, session.getReplacedCount());
            assertNotNull(session.getCurrentMatch());
            assertEquals(8, session.getCurrentMatch().start());
        }

        @Test
        void 全てスキップすると置換なしで終了する() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "baz");
            session.start();

            session.skipCurrent();
            session.skipCurrent();

            assertTrue(session.isFinished());
            assertEquals("foo bar foo", window.getBuffer().getText());
            assertEquals(0, session.getReplacedCount());
        }
    }

    @Nested
    class 一括置換 {

        @Test
        void 残り全てのマッチが置換される() {
            var window = windowOf("foo bar foo baz foo");
            var session = literal(window, messageBuffer, controller, "foo", "X");
            session.start();

            session.replaceAllRemaining();

            assertEquals("X bar X baz X", window.getBuffer().getText());
            assertEquals(3, session.getReplacedCount());
            assertTrue(session.isFinished());
        }

        @Test
        void yで1件置換後の一括置換は残りだけ置換する() {
            var window = windowOf("foo foo foo");
            var session = literal(window, messageBuffer, controller, "foo", "X");
            session.start();

            session.replaceCurrent();
            session.replaceAllRemaining();

            assertEquals("X X X", window.getBuffer().getText());
            assertEquals(3, session.getReplacedCount());
        }

        @Test
        void 外側withTransaction経由なら一括置換は1undo単位にまとまる() {
            // CommandLoop 経由で呼ばれた時の挙動を模倣する
            var window = windowOf("foo foo foo");
            var buffer = window.getBuffer();
            var session = literal(window, messageBuffer, controller, "foo", "X");
            session.start();

            buffer.getUndoManager().withTransaction(session::replaceAllRemaining);
            assertEquals("X X X", buffer.getText());

            var inverse = buffer.getUndoManager().undo().orElseThrow();
            buffer.apply(inverse);

            assertEquals("foo foo foo", buffer.getText());
        }
    }

    @Nested
    class キャンセル {

        @Test
        void キャンセル時にoverridingKeymapがクリアされる() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "baz");
            session.start();

            session.cancel();

            assertTrue(session.isFinished());
            assertTrue(controller.cleared);
            assertTrue(messageBuffer.getLastMessage().orElse("").equals("Quit"));
        }

        @Test
        void キャンセル前の置換は残る() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "baz");
            session.start();

            session.replaceCurrent();
            session.cancel();

            assertEquals("baz bar foo", window.getBuffer().getText());
            assertEquals(1, session.getReplacedCount());
        }
    }

    @Nested
    class リージョン指定 {

        @Test
        void リージョン内のマッチのみ置換される() {
            var window = windowOf("foo bar foo baz foo");
            // "foo bar foo" の範囲 [0, 11] で置換
            var session = literalInRegion(window, messageBuffer, controller, "foo", "X", 0, 11);
            session.start();

            session.replaceAllRemaining();

            assertEquals("X bar X baz foo", window.getBuffer().getText());
            assertEquals(2, session.getReplacedCount());
        }

        @Test
        void リージョン終了後にmarkがクリアされる() {
            var window = windowOf("foo bar foo");
            window.setMark(0);
            window.setPoint(11);
            var session = literalInRegion(window, messageBuffer, controller, "foo", "X", 0, 11);
            session.start();

            session.replaceAllRemaining();

            assertTrue(window.getMark().isEmpty());
        }
    }

    @Nested
    class 正規表現置換 {

        @Test
        void マッチ全体の展開() {
            var window = windowOf("abc 123 def 456");
            var session = regexp(window, messageBuffer, controller, "\\d+", "[\\&]");
            session.start();

            session.replaceAllRemaining();

            assertEquals("abc [123] def [456]", window.getBuffer().getText());
            assertEquals(2, session.getReplacedCount());
        }

        @Test
        void キャプチャグループの展開() {
            var window = windowOf("a=1, b=2");
            var session = regexp(window, messageBuffer, controller, "(\\w+)=(\\d+)", "\\2=\\1");
            session.start();

            session.replaceAllRemaining();

            assertEquals("1=a, 2=b", window.getBuffer().getText());
        }

        @Test
        void 置換文字列にマッチが含まれていても無限ループしない() {
            // TO に FROM が含まれる場合、searchFrom は replacement 後に前進するのでループしない
            var window = windowOf("ab ab");
            var session = regexp(window, messageBuffer, controller, "ab", "abab");
            session.start();

            session.replaceAllRemaining();

            assertEquals("abab abab", window.getBuffer().getText());
            assertEquals(2, session.getReplacedCount());
        }
    }

    @Nested
    class CommandLoop互換性 {

        // CommandLoop は command.execute を UndoManager#withTransaction で包む。
        // セッションが内部でさらに withTransaction を開くと IllegalStateException になるため、
        // 外側で withTransaction を張った状態で y / ! を呼んでも例外が出ないことを確認する。

        @Test
        void 外側withTransaction中にreplaceCurrentが例外を投げない() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "X");
            session.start();

            window.getBuffer().getUndoManager().withTransaction(session::replaceCurrent);

            assertEquals("X bar foo", window.getBuffer().getText());
        }

        @Test
        void 外側withTransaction中にreplaceAllRemainingが例外を投げない() {
            var window = windowOf("foo bar foo");
            var session = literal(window, messageBuffer, controller, "foo", "X");
            session.start();

            window.getBuffer().getUndoManager().withTransaction(session::replaceAllRemaining);

            assertEquals("X bar X", window.getBuffer().getText());
        }
    }

    @Nested
    class ポイント復元 {

        @Test
        void 終了時にポイントが開始時の位置に戻る() {
            var window = windowOf("foo bar foo");
            window.setPoint(4);
            var session = QueryReplaceSession.forLiteral(window, messageBuffer, controller, "foo", "baz", 4, 11, false);
            session.start();
            assertEquals(8, window.getPoint()); // マッチ先頭 8 に移動

            session.replaceAllRemaining();

            assertEquals(4, window.getPoint());
        }
    }
}
