package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.concurrent.ActorThread;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferActorTest {

    private final MutableList<BufferActor> createdActors = Lists.mutable.empty();

    @AfterEach
    void tearDown() {
        createdActors.forEach(BufferActor::shutdown);
    }

    private BufferActor createActor(String name) {
        var actor = new BufferActor(new EditableBuffer(name, new GapTextModel()), ActorThread.create("test-" + name));
        createdActors.add(actor);
        return actor;
    }

    private BufferActor createActorWithText(String name, String text) {
        var model = new GapTextModel();
        model.insert(0, text);
        var actor = new BufferActor(new EditableBuffer(name, model), ActorThread.create("test-" + name));
        createdActors.add(actor);
        return actor;
    }

    @Nested
    class テキスト操作 {

        @Test
        void テキストを挿入できる() {
            var actor = createActor("test");
            actor.insertText(0, "hello").join();
            assertEquals("hello", actor.getText().join());
        }

        @Test
        void テキストを削除できる() {
            var actor = createActorWithText("test", "hello");
            actor.deleteText(0, 2).join();
            assertEquals("llo", actor.getText().join());
        }

        @Test
        void TextChangeを適用できる() {
            var actor = createActor("test");
            var change = new TextChange.Insert(0, "hello");
            actor.apply(change).join();
            assertEquals("hello", actor.getText().join());
        }

        @Test
        void 部分文字列を取得できる() {
            var actor = createActorWithText("test", "hello world");
            var sub = actor.substring(0, 5).join();
            assertEquals("hello", sub);
        }
    }

    @Nested
    class メタデータ操作 {

        @Test
        void バッファ名を取得できる() {
            var actor = createActor("test-buffer");
            assertEquals("test-buffer", actor.getName().join());
        }

        @Test
        void ファイルパスを設定して取得できる() {
            var actor = createActor("test");
            var path = Path.of("/tmp/test.txt");
            actor.setFilePath(path).join();
            assertTrue(actor.getFilePath().join().isPresent());
            assertEquals(path, actor.getFilePath().join().get());
        }

        @Test
        void dirtyフラグを操作できる() {
            var actor = createActor("test");
            assertFalse(actor.isDirty().join());
            actor.markDirty().join();
            assertTrue(actor.isDirty().join());
            actor.markClean().join();
            assertFalse(actor.isDirty().join());
        }
    }

    @Nested
    class 行操作 {

        @Test
        void 行数を取得できる() {
            var actor = createActorWithText("test", "line1\nline2\nline3");
            assertEquals(3, actor.lineCount().join());
        }

        @Test
        void オフセットから行インデックスを取得できる() {
            var actor = createActorWithText("test", "line1\nline2");
            assertEquals(1, actor.lineIndexForOffset(6).join());
        }

        @Test
        void 行の先頭オフセットを取得できる() {
            var actor = createActorWithText("test", "line1\nline2");
            assertEquals(6, actor.lineStartOffset(1).join());
        }

        @Test
        void 行テキストを取得できる() {
            var actor = createActorWithText("test", "line1\nline2");
            assertEquals("line2", actor.lineText(1).join());
        }
    }

    @Nested
    class 直接アクセス {

        @Test
        void ラップしているBufferを取得できる() {
            var buffer = new EditableBuffer("test", new GapTextModel());
            var actor = new BufferActor(buffer, ActorThread.create("test-direct"));
            createdActors.add(actor);
            assertEquals(buffer, actor.getBuffer());
        }
    }
}
