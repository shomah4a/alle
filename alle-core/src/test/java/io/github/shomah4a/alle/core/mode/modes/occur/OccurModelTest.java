package io.github.shomah4a.alle.core.mode.modes.occur;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OccurModelTest {

    private static BufferFacade createBuffer(String name, String text) {
        var settings = new SettingsRegistry();
        var buffer = new TextBuffer(name, new GapTextModel(), settings);
        var facade = new BufferFacade(buffer);
        if (!text.isEmpty()) {
            facade.insertText(0, text);
        }
        return facade;
    }

    @Nested
    class search {

        @Test
        void マッチする行のみが結果に含まれる() {
            var buffer = createBuffer("test.txt", "foo\nbar\nfoo bar\nbaz");
            var model = OccurModel.search(buffer, "foo");

            assertEquals(2, model.matchCount());
            assertEquals(0, model.getMatches().get(0).lineIndex());
            assertEquals("foo", model.getMatches().get(0).lineText());
            assertEquals(2, model.getMatches().get(1).lineIndex());
            assertEquals("foo bar", model.getMatches().get(1).lineText());
        }

        @Test
        void マッチ開始位置がコードポイント単位で記録される() {
            var buffer = createBuffer("test.txt", "hello world");
            var model = OccurModel.search(buffer, "world");

            assertEquals(1, model.matchCount());
            assertEquals(6, model.getMatches().get(0).matchOffsetInLine());
        }

        @Test
        void クエリが空の場合はマッチなし() {
            var buffer = createBuffer("test.txt", "foo\nbar");
            var model = OccurModel.search(buffer, "");

            assertEquals(0, model.matchCount());
        }

        @Test
        void マッチが1件もない場合は空のリスト() {
            var buffer = createBuffer("test.txt", "foo\nbar");
            var model = OccurModel.search(buffer, "xyz");

            assertEquals(0, model.matchCount());
        }

        @Test
        void 元バッファ名とクエリが保持される() {
            var buffer = createBuffer("my-file.txt", "foo");
            var model = OccurModel.search(buffer, "foo");

            assertEquals("my-file.txt", model.getSourceBufferName());
            assertEquals("foo", model.getQuery());
        }

        @Test
        void 同一行に複数マッチがあっても行は1回のみ記録される() {
            var buffer = createBuffer("test.txt", "foo foo foo");
            var model = OccurModel.search(buffer, "foo");

            assertEquals(1, model.matchCount());
            assertEquals(0, model.getMatches().get(0).matchOffsetInLine());
        }

        @Test
        void サロゲートペアを含む行でもオフセットがコードポイント単位で正しい() {
            var buffer = createBuffer("test.txt", "\uD83D\uDE00hello");
            var model = OccurModel.search(buffer, "hello");

            assertEquals(1, model.matchCount());
            assertEquals(1, model.getMatches().get(0).matchOffsetInLine());
        }
    }

    @Nested
    class smartCase {

        @Test
        void 小文字クエリは大文字小文字を区別しない() {
            var buffer = createBuffer("test.txt", "Hello\nworld\nHELLO");
            var model = OccurModel.search(buffer, "hello");

            assertEquals(2, model.matchCount());
            assertEquals(0, model.getMatches().get(0).lineIndex());
            assertEquals(2, model.getMatches().get(1).lineIndex());
        }

        @Test
        void 大文字を含むクエリは大文字小文字を区別する() {
            var buffer = createBuffer("test.txt", "Hello\nworld\nhello");
            var model = OccurModel.search(buffer, "Hello");

            assertEquals(1, model.matchCount());
            assertEquals(0, model.getMatches().get(0).lineIndex());
        }

        @Test
        void ケース無視時もマッチ開始位置がコードポイント単位で正しい() {
            var buffer = createBuffer("test.txt", "😀Hello");
            var model = OccurModel.search(buffer, "hello");

            assertEquals(1, model.matchCount());
            // 絵文字 1 コードポイント分ぶん右の位置
            assertEquals(1, model.getMatches().get(0).matchOffsetInLine());
        }

        @Test
        void ケース無視時も同一行に複数マッチがあっても行は1回のみ記録される() {
            var buffer = createBuffer("test.txt", "Hello hello HELLO");
            var model = OccurModel.search(buffer, "hello");

            assertEquals(1, model.matchCount());
            // 最初の Hello のオフセットを採用
            assertEquals(0, model.getMatches().get(0).matchOffsetInLine());
        }
    }
}
