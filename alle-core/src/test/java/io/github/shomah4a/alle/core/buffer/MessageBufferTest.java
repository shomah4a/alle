package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MessageBufferTest {

    private MessageBuffer createBuffer() {
        return new MessageBuffer("*Messages*", 100);
    }

    @Nested
    class 基本プロパティ {

        @Test
        void 名前がMessagesである() {
            var buffer = createBuffer();
            assertEquals("*Messages*", buffer.getName());
        }

        @Test
        void 読み取り専用である() {
            var buffer = createBuffer();
            assertTrue(buffer.isReadOnly());
        }

        @Test
        void ファイルパスがない() {
            var buffer = createBuffer();
            assertEquals(Optional.empty(), buffer.getFilePath());
        }

        @Test
        void dirtyにならない() {
            var buffer = createBuffer();
            assertFalse(buffer.isDirty());
            buffer.markDirty();
            assertFalse(buffer.isDirty());
        }
    }

    @Nested
    class メッセージ追加 {

        @Test
        void メッセージを追加すると行数が増える() {
            var buffer = createBuffer();
            assertEquals(1, buffer.lineCount()); // 空でも1行
            assertEquals("", buffer.lineText(0));

            buffer.message("hello");
            assertEquals(1, buffer.lineCount());
            assertEquals("hello", buffer.lineText(0));

            buffer.message("world");
            assertEquals(2, buffer.lineCount());
            assertEquals("hello", buffer.lineText(0));
            assertEquals("world", buffer.lineText(1));
        }

        @Test
        void 最後のメッセージを取得できる() {
            var buffer = createBuffer();
            assertEquals(Optional.empty(), buffer.getLastMessage());

            buffer.message("first");
            assertEquals(Optional.of("first"), buffer.getLastMessage());

            buffer.message("second");
            assertEquals(Optional.of("second"), buffer.getLastMessage());
        }

        @Test
        void 容量を超えると古いメッセージが消える() {
            var buffer = new MessageBuffer("*Messages*", 3);
            buffer.message("a");
            buffer.message("b");
            buffer.message("c");
            buffer.message("d");

            assertEquals(3, buffer.lineCount());
            assertEquals("b", buffer.lineText(0));
            assertEquals("c", buffer.lineText(1));
            assertEquals("d", buffer.lineText(2));
        }
    }

    @Nested
    class テキスト読み取り {

        @Test
        void 空バッファのlengthは0() {
            var buffer = createBuffer();
            assertEquals(0, buffer.length());
        }

        @Test
        void メッセージ追加後のlengthは正しい() {
            var buffer = createBuffer();
            buffer.message("abc");
            assertEquals(3, buffer.length());

            buffer.message("de");
            // "abc\nde" = 6
            assertEquals(6, buffer.length());
        }

        @Test
        void getTextが全メッセージを改行区切りで返す() {
            var buffer = createBuffer();
            assertEquals("", buffer.getText());

            buffer.message("hello");
            assertEquals("hello", buffer.getText());

            buffer.message("world");
            assertEquals("hello\nworld", buffer.getText());
        }

        @Test
        void lineStartOffsetが正しい() {
            var buffer = createBuffer();
            buffer.message("abc");
            buffer.message("de");
            buffer.message("f");

            assertEquals(0, buffer.lineStartOffset(0));
            assertEquals(4, buffer.lineStartOffset(1)); // "abc" + "\n"
            assertEquals(7, buffer.lineStartOffset(2)); // "abc\nde" + "\n"
        }

        @Test
        void lineIndexForOffsetが正しい() {
            var buffer = createBuffer();
            buffer.message("abc");
            buffer.message("de");

            assertEquals(0, buffer.lineIndexForOffset(0)); // 'a'
            assertEquals(0, buffer.lineIndexForOffset(2)); // 'c'
            assertEquals(0, buffer.lineIndexForOffset(3)); // 'c'の後（行末）
            assertEquals(1, buffer.lineIndexForOffset(4)); // 'd'
            assertEquals(1, buffer.lineIndexForOffset(5)); // 'e'
        }

        @Test
        void 全角文字を含むメッセージのlengthが正しい() {
            var buffer = createBuffer();
            buffer.message("あいう");
            assertEquals(3, buffer.length());
        }
    }

    @Nested
    class 書き込み禁止 {

        @Test
        void insertTextで例外が発生する() {
            var buffer = createBuffer();
            assertThrows(ReadOnlyBufferException.class, () -> buffer.insertText(0, "x"));
        }

        @Test
        void deleteTextで例外が発生する() {
            var buffer = createBuffer();
            assertThrows(ReadOnlyBufferException.class, () -> buffer.deleteText(0, 1));
        }

        @Test
        void applyで例外が発生する() {
            var buffer = createBuffer();
            assertThrows(ReadOnlyBufferException.class, () -> buffer.apply(new TextChange.Insert(0, "x")));
        }
    }

    @Nested
    class エコーエリア表示フラグ {

        @Test
        void 初期状態では表示フラグがfalse() {
            var buffer = createBuffer();
            assertFalse(buffer.isShowingMessage());
        }

        @Test
        void メッセージ追加で表示フラグがtrue() {
            var buffer = createBuffer();
            buffer.message("hello");
            assertTrue(buffer.isShowingMessage());
        }

        @Test
        void クリアで表示フラグがfalse() {
            var buffer = createBuffer();
            buffer.message("hello");
            buffer.clearShowingMessage();
            assertFalse(buffer.isShowingMessage());
        }
    }
}
