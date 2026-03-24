package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.shomah4a.alle.core.buffer.BufferActor;
import io.github.shomah4a.alle.core.buffer.EditableBuffer;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WindowActorTest {

    private WindowActor createActor() {
        var buffer = new EditableBuffer("test", new GapTextModel());
        return new WindowActor(new Window(buffer));
    }

    private WindowActor createActorWithText(String text) {
        var model = new GapTextModel();
        model.insert(0, text);
        var buffer = new EditableBuffer("test", model);
        var window = new Window(buffer);
        return new WindowActor(window);
    }

    @Nested
    class テキスト操作 {

        @Test
        void テキストを挿入できる() {
            var actor = createActor();
            actor.insert("hello").join();
            assertEquals("hello", actor.getBuffer().join().getText());
            assertEquals(5, actor.getPoint().join());
        }

        @Test
        void 前方を削除できる() {
            var actor = createActorWithText("hello");
            actor.setPoint(5).join();
            actor.deleteBackward(2).join();
            assertEquals("hel", actor.getBuffer().join().getText());
            assertEquals(3, actor.getPoint().join());
        }

        @Test
        void 後方を削除できる() {
            var actor = createActorWithText("hello");
            actor.setPoint(0).join();
            actor.deleteForward(2).join();
            assertEquals("llo", actor.getBuffer().join().getText());
            assertEquals(0, actor.getPoint().join());
        }
    }

    @Nested
    class カーソル操作 {

        @Test
        void ポイントを設定して取得できる() {
            var actor = createActorWithText("hello");
            actor.setPoint(3).join();
            assertEquals(3, actor.getPoint().join());
        }

        @Test
        void 表示開始行を設定して取得できる() {
            var actor = createActorWithText("line1\nline2\nline3");
            actor.setDisplayStartLine(1).join();
            assertEquals(1, actor.getDisplayStartLine().join());
        }
    }

    @Nested
    class バッファ操作 {

        @Test
        void バッファを切り替えられる() {
            var actor = createActor();
            var newBuffer = new EditableBuffer("new-buffer", new GapTextModel());
            actor.setBuffer(newBuffer).join();
            assertEquals("new-buffer", actor.getBuffer().join().getName());
            assertEquals(0, actor.getPoint().join());
        }
    }

    @Nested
    class BufferActor管理 {

        @Test
        void コンストラクタで渡したBufferActorを取得できる() {
            var buffer = new EditableBuffer("test", new GapTextModel());
            var bufferActor = new BufferActor(buffer);
            var actor = new WindowActor(new Window(buffer), bufferActor);
            assertSame(bufferActor, actor.getBufferActor());
        }

        @Test
        void BufferActorでバッファを差し替えられる() {
            var actor = createActor();
            var newBuffer = new EditableBuffer("new-buffer", new GapTextModel());
            var newActor = new BufferActor(newBuffer);
            actor.setBuffer(newActor).join();
            assertSame(newActor, actor.getBufferActor());
            assertEquals("new-buffer", actor.getBuffer().join().getName());
        }

        @Test
        void 引数なしコンストラクタでもBufferActorが生成される() {
            var buffer = new EditableBuffer("test", new GapTextModel());
            var actor = new WindowActor(new Window(buffer));
            assertEquals("test", actor.getBufferActor().getName().join());
        }
    }

    @Nested
    class 直接アクセス {

        @Test
        void ラップしているWindowを取得できる() {
            var buffer = new EditableBuffer("test", new GapTextModel());
            var window = new Window(buffer);
            var actor = new WindowActor(window);
            assertEquals(window, actor.getWindow());
        }
    }
}
