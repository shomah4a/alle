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
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.Window;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ISearchSessionTest {

    private static final SettingsRegistry SETTINGS = new SettingsRegistry();

    private Window window;
    private MessageBuffer messageBuffer;
    private TestOverridingKeymapController controller;
    private ISearchHistory history;

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

    @BeforeEach
    void setUp() {
        history = new ISearchHistory();
        var model = new GapTextModel();
        model.insert(0, "hello world hello");
        var buffer = new BufferFacade(new TextBuffer("test", model, SETTINGS));
        window = new Window(buffer);
        window.setPoint(0);
        messageBuffer = new MessageBuffer("*Messages*", 100, SETTINGS);
        controller = new TestOverridingKeymapController();
    }

    private ISearchSession createForwardSession() {
        return new ISearchSession(window, messageBuffer, controller, history, true);
    }

    private ISearchSession createBackwardSession() {
        return new ISearchSession(window, messageBuffer, controller, history, false);
    }

    private void replaceBuffer(String text) {
        var model = new GapTextModel();
        model.insert(0, text);
        var buffer = new BufferFacade(new TextBuffer("test", model, SETTINGS));
        window = new Window(buffer);
        window.setPoint(0);
    }

    @Nested
    class 前方検索の基本 {

        @Test
        void 開始時にoveridingKeymapが設定される() {
            var session = createForwardSession();
            session.start();

            assertNotNull(controller.currentKeymap);
            assertFalse(controller.cleared);
        }

        @Test
        void 文字追加で最初のマッチに移動しカーソルがマッチ末尾に置かれる() {
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');

            assertEquals("hel", session.getQuery());
            assertNotNull(session.getCurrentMatch());
            assertEquals(0, session.getCurrentMatch().start());
            assertEquals(3, session.getCurrentMatch().end());
            // forward検索ではカーソルがマッチ末尾に置かれる
            assertEquals(3, window.getPoint());
        }

        @Test
        void マッチ位置にISEARCH_MATCHフェイスが付与される() {
            var session = createForwardSession();
            session.start();

            session.appendChar('w');
            session.appendChar('o');

            var spans = window.getBuffer().getFaceSpans(0, 17);
            assertTrue(spans.anySatisfy(s -> s.faceName().equals(FaceName.ISEARCH_MATCH)));
        }

        @Test
        void マッチがない場合はfailedになる() {
            var session = createForwardSession();
            session.start();

            session.appendChar('x');
            session.appendChar('y');
            session.appendChar('z');

            assertNull(session.getCurrentMatch());
            assertTrue(messageBuffer.getLastMessage().orElse("").startsWith("Failing"));
        }
    }

    @Nested
    class 後方検索の基本 {

        @Test
        void 後方検索でカーソルがマッチ先頭に置かれる() {
            window.setPoint(17); // バッファ末尾
            var session = createBackwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');

            assertNotNull(session.getCurrentMatch());
            assertEquals(12, session.getCurrentMatch().start());
            assertEquals(15, session.getCurrentMatch().end());
            // backward検索ではカーソルがマッチ先頭に置かれる
            assertEquals(12, window.getPoint());
        }
    }

    @Nested
    class 次と前のマッチ {

        @Test
        void searchNextで次のマッチに移動する() {
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            // 最初のマッチ: [0,3)
            var match1 = session.getCurrentMatch();
            assertNotNull(match1);
            assertEquals(0, match1.start());

            session.searchNext();
            // 次のマッチ: [12,15)
            var match2 = session.getCurrentMatch();
            assertNotNull(match2);
            assertEquals(12, match2.start());
        }

        @Test
        void searchPreviousで前のマッチに移動する() {
            window.setPoint(17);
            var session = createBackwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            // 最初のマッチ（後方）: [12,15)
            var match1 = session.getCurrentMatch();
            assertNotNull(match1);
            assertEquals(12, match1.start());

            session.searchPrevious();
            // 前のマッチ: [0,3)
            var match2 = session.getCurrentMatch();
            assertNotNull(match2);
            assertEquals(0, match2.start());
        }
    }

    @Nested
    class ラップアラウンド {

        @Test
        void 末尾到達後にラップアラウンドで先頭から検索する() {
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            // [0,3)
            session.searchNext(); // [12,15)
            session.searchNext(); // ラップアラウンドで [0,3)

            assertNotNull(session.getCurrentMatch());
            assertEquals(0, session.getCurrentMatch().start());
            assertTrue(session.getCurrentMatch().wrapped());
            assertTrue(messageBuffer.getLastMessage().orElse("").startsWith("Wrapped"));
        }
    }

    @Nested
    class 確定とキャンセル {

        @Test
        void 確定でカーソルが現在位置に残りoveridingKeymapがクリアされる() {
            var session = createForwardSession();
            session.start();

            session.appendChar('w');
            session.appendChar('o');
            session.appendChar('r');
            // "wor" にマッチ → カーソルは末尾(9)
            int pointBeforeConfirm = window.getPoint();

            session.confirm();

            assertEquals(pointBeforeConfirm, window.getPoint());
            assertTrue(controller.cleared);
            // ハイライトが除去されていること
            var spans = window.getBuffer().getFaceSpans(0, 17);
            assertFalse(spans.anySatisfy(s -> s.faceName().equals(FaceName.ISEARCH_MATCH)));
        }

        @Test
        void キャンセルでカーソルが元の位置に戻りoveridingKeymapがクリアされる() {
            window.setPoint(5);
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            // カーソルが移動しているはず

            session.cancel();

            assertEquals(5, window.getPoint());
            assertTrue(controller.cleared);
        }
    }

    @Nested
    class 文字削除 {

        @Test
        void 文字削除でクエリが短くなり再検索される() {
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            session.appendChar('l');
            session.appendChar('o');
            assertEquals("hello", session.getQuery());

            session.deleteChar();
            assertEquals("hell", session.getQuery());
            assertNotNull(session.getCurrentMatch());
        }

        @Test
        void 全文字削除でクエリが空になりカーソルが元の位置に戻る() {
            window.setPoint(5);
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.deleteChar();

            assertEquals("", session.getQuery());
            assertEquals(5, window.getPoint());
            assertNull(session.getCurrentMatch());
        }
    }

    @Nested
    class 前回クエリの再検索 {

        @Test
        void 空クエリでsearchNextすると前回クエリが使用される() {
            // まず検索して確定し、lastQueryを設定
            var session1 = createForwardSession();
            session1.start();
            session1.appendChar('w');
            session1.appendChar('o');
            session1.confirm();

            // 新しいセッションで空クエリのままC-s
            window.setPoint(0);
            var session2 = createForwardSession();
            session2.start();
            session2.searchNext();

            assertEquals("wo", session2.getQuery());
            assertNotNull(session2.getCurrentMatch());
            assertEquals(6, session2.getCurrentMatch().start());
        }
    }

    @Nested
    class smartCase {

        @Test
        void 全て小文字のクエリは大文字小文字を区別せずマッチする() {
            replaceBuffer("Hello World HELLO");
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            session.appendChar('l');
            session.appendChar('o');

            // 先頭の "Hello" にマッチすること
            assertNotNull(session.getCurrentMatch());
            assertEquals(0, session.getCurrentMatch().start());
            assertEquals(5, session.getCurrentMatch().end());
        }

        @Test
        void クエリに大文字が含まれるなら大文字小文字を区別する() {
            replaceBuffer("Hello World hello");
            var session = createForwardSession();
            session.start();

            session.appendChar('H');
            session.appendChar('e');
            session.appendChar('l');
            session.appendChar('l');
            session.appendChar('o');

            // 先頭の "Hello" にマッチし、末尾の "hello" にはマッチしない範囲
            assertNotNull(session.getCurrentMatch());
            assertEquals(0, session.getCurrentMatch().start());
            assertEquals(5, session.getCurrentMatch().end());
        }

        @Test
        void 大文字クエリは小文字テキストにマッチしない() {
            replaceBuffer("hello world");
            var session = createForwardSession();
            session.start();

            session.appendChar('H');
            session.appendChar('e');
            session.appendChar('l');
            session.appendChar('l');
            session.appendChar('o');

            // case sensitive 扱いになるため "Hello" は見つからない
            assertNull(session.getCurrentMatch());
            assertTrue(messageBuffer.getLastMessage().orElse("").startsWith("Failing"));
        }

        @Test
        void searchNextで連続したマッチを順に巡回する() {
            replaceBuffer("Hello hello HELLO");
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            session.appendChar('l');
            session.appendChar('o');
            // 1 つ目の Hello (0-5)
            var match1 = session.getCurrentMatch();
            assertNotNull(match1);
            assertEquals(0, match1.start());

            session.searchNext();
            var match2 = session.getCurrentMatch();
            assertNotNull(match2);
            assertEquals(6, match2.start());

            session.searchNext();
            var match3 = session.getCurrentMatch();
            assertNotNull(match3);
            assertEquals(12, match3.start());
        }

        @Test
        void ケース無視マッチでもハイライト幅がクエリ長と一致する() {
            replaceBuffer("HELLO world");
            var session = createForwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            session.appendChar('l');
            session.appendChar('o');

            var match = session.getCurrentMatch();
            assertNotNull(match);
            // クエリは小文字 "hello" (5 codepoint)、テキストは大文字 "HELLO"。
            // ケース無視でマッチし、ハイライト範囲はクエリ長と一致する [0, 5)。
            assertEquals(0, match.start());
            assertEquals(5, match.end());

            var spans = window.getBuffer().getFaceSpans(0, 11);
            var matchSpans = spans.select(s -> s.faceName().equals(FaceName.ISEARCH_MATCH));
            assertEquals(1, matchSpans.size());
            assertEquals(0, matchSpans.get(0).start());
            assertEquals(5, matchSpans.get(0).end());
        }

        @Test
        void searchPreviousで同位置を返し続けない() {
            replaceBuffer("Hello hello HELLO");
            window.setPoint(17);
            var session = createBackwardSession();
            session.start();

            session.appendChar('h');
            session.appendChar('e');
            session.appendChar('l');
            session.appendChar('l');
            session.appendChar('o');
            // 末尾の HELLO (12-17) にマッチ
            var match1 = session.getCurrentMatch();
            assertNotNull(match1);
            assertEquals(12, match1.start());

            session.searchPrevious();
            var match2 = session.getCurrentMatch();
            assertNotNull(match2);
            assertEquals(6, match2.start());

            session.searchPrevious();
            var match3 = session.getCurrentMatch();
            assertNotNull(match3);
            assertEquals(0, match3.start());
        }
    }

    @Nested
    class エコー表示 {

        @Test
        void 前方検索のエコー表示がI_searchプレフィックスを持つ() {
            var session = createForwardSession();
            session.start();

            session.appendChar('h');

            assertTrue(messageBuffer.getLastMessage().orElse("").contains("I-search: h"));
        }

        @Test
        void 後方検索のエコー表示がI_search_backwardプレフィックスを持つ() {
            window.setPoint(17);
            var session = createBackwardSession();
            session.start();

            session.appendChar('h');

            assertTrue(messageBuffer.getLastMessage().orElse("").contains("I-search backward: h"));
        }
    }
}
