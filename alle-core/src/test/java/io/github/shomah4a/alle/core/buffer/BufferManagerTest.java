package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BufferManagerTest {

    private BufferFacade createBuffer(String name) {
        return new BufferFacade(new TextBuffer(name, new GapTextModel(), new SettingsRegistry()));
    }

    private BufferFacade createBufferWithPath(String name, Path path) {
        return new BufferFacade(new TextBuffer(name, new GapTextModel(), new SettingsRegistry(), path));
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

    @Nested
    class バッファ名のuniquify {

        @Test
        void 同名ファイルを追加するとバッファ名がuniquifyされる() {
            var manager = new BufferManager();
            var buf1 = createBufferWithPath("main.py", Path.of("/home/user/project1/main.py"));
            var buf2 = createBufferWithPath("main.py", Path.of("/home/user/project2/main.py"));
            manager.add(buf1);
            manager.add(buf2);

            assertEquals("project1/main.py", buf1.getName());
            assertEquals("project2/main.py", buf2.getName());
        }

        @Test
        void 同名バッファが1つになるとファイル名のみに戻る() {
            var manager = new BufferManager();
            var buf1 = createBufferWithPath("main.py", Path.of("/home/user/project1/main.py"));
            var buf2 = createBufferWithPath("main.py", Path.of("/home/user/project2/main.py"));
            manager.add(buf1);
            manager.add(buf2);

            manager.remove("project2/main.py");
            assertEquals("main.py", buf1.getName());
        }

        @Test
        void ファイル名が異なるバッファはuniquifyされない() {
            var manager = new BufferManager();
            var buf1 = createBufferWithPath("foo.txt", Path.of("/home/user/a/foo.txt"));
            var buf2 = createBufferWithPath("bar.txt", Path.of("/home/user/a/bar.txt"));
            manager.add(buf1);
            manager.add(buf2);

            assertEquals("foo.txt", buf1.getName());
            assertEquals("bar.txt", buf2.getName());
        }

        @Test
        void ファイルパスを持たないバッファはuniquifyの対象外() {
            var manager = new BufferManager();
            var scratch = createBuffer("*scratch*");
            var buf1 = createBufferWithPath("main.py", Path.of("/home/user/project1/main.py"));
            manager.add(scratch);
            manager.add(buf1);

            assertEquals("*scratch*", scratch.getName());
            assertEquals("main.py", buf1.getName());
        }
    }
}
