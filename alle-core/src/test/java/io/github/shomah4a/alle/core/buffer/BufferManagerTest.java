package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferManagerTest {

    private BufferFacade createBuffer(String name) {
        return new BufferFacade(new TextBuffer(name, new GapTextModel(), new SettingsRegistry()));
    }

    @Nested
    class 初期状態 {

        @Test
        void バッファが空でcurrentはempty() {
            var manager = new BufferManager();
            assertTrue(manager.current().isEmpty());
            assertEquals(0, manager.size());
        }
    }

    @Nested
    class 追加 {

        @Test
        void バッファを追加すると現在のバッファになる() {
            var manager = new BufferManager();
            var buf = createBuffer("buf1");
            manager.add(buf);
            assertEquals("buf1", manager.current().orElseThrow().getName());
            assertEquals(1, manager.size());
        }

        @Test
        void 複数追加すると最後に追加したものが現在のバッファになる() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            manager.add(createBuffer("buf2"));
            assertEquals("buf2", manager.current().orElseThrow().getName());
            assertEquals(2, manager.size());
        }
    }

    @Nested
    class 検索 {

        @Test
        void 名前でバッファを検索できる() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            manager.add(createBuffer("buf2"));
            assertEquals("buf1", manager.findByName("buf1").orElseThrow().getName());
        }

        @Test
        void 存在しない名前ではemptyを返す() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            assertTrue(manager.findByName("notexist").isEmpty());
        }
    }

    @Nested
    class 切り替え {

        @Test
        void 名前を指定して現在のバッファを切り替えられる() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            manager.add(createBuffer("buf2"));
            assertTrue(manager.switchTo("buf1"));
            assertEquals("buf1", manager.current().orElseThrow().getName());
        }

        @Test
        void 存在しない名前での切り替えは失敗する() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            assertFalse(manager.switchTo("notexist"));
            assertEquals("buf1", manager.current().orElseThrow().getName());
        }
    }

    @Nested
    class 削除 {

        @Test
        void バッファを削除できる() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            manager.add(createBuffer("buf2"));
            assertTrue(manager.remove("buf1"));
            assertEquals(1, manager.size());
            assertFalse(manager.findByName("buf1").isPresent());
        }

        @Test
        void 現在のバッファを削除すると前のバッファに切り替わる() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            manager.add(createBuffer("buf2"));
            manager.remove("buf2");
            assertEquals("buf1", manager.current().orElseThrow().getName());
        }

        @Test
        void 全バッファを削除するとcurrentはempty() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            manager.remove("buf1");
            assertTrue(manager.current().isEmpty());
        }

        @Test
        void 存在しない名前の削除は失敗する() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            assertFalse(manager.remove("notexist"));
            assertEquals(1, manager.size());
        }
    }

    @Nested
    class バッファ一覧 {

        @Test
        void 全バッファの読み取り専用リストを返す() {
            var manager = new BufferManager();
            manager.add(createBuffer("buf1"));
            manager.add(createBuffer("buf2"));
            var list = manager.getBuffers();
            assertEquals(2, list.size());
            assertEquals("buf1", list.get(0).getName());
            assertEquals("buf2", list.get(1).getName());
        }
    }
}
