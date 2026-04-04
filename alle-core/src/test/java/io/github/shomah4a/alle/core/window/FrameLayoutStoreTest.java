package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FrameLayoutStoreTest {

    private FrameSnapshot createDummySnapshot(String bufferName) {
        var current = new BufferHistoryEntry(new BufferIdentifier.ByName(bufferName), ViewState.INITIAL);
        var windowSnapshot = new WindowSnapshot(current, Lists.immutable.empty(), false);
        var tree = new WindowTreeSnapshot.Leaf(windowSnapshot);
        return new FrameSnapshot(tree, 0);
    }

    @Nested
    class save {

        @Test
        void 名前付きでスナップショットを保存できる() {
            var store = new FrameLayoutStore();
            var snapshot = createDummySnapshot("test");

            store.save("layout1", snapshot);

            assertTrue(store.load("layout1").isPresent());
        }

        @Test
        void 同名で上書き保存できる() {
            var store = new FrameLayoutStore();
            store.save("layout1", createDummySnapshot("old"));
            store.save("layout1", createDummySnapshot("new"));

            var loaded = store.load("layout1").orElseThrow();
            var leaf = (WindowTreeSnapshot.Leaf) loaded.tree();
            assertEquals(
                    new BufferIdentifier.ByName("new"),
                    leaf.snapshot().current().identifier());
        }
    }

    @Nested
    class load {

        @Test
        void 存在しない名前はemptyを返す() {
            var store = new FrameLayoutStore();

            assertFalse(store.load("nonexistent").isPresent());
        }
    }

    @Nested
    class names {

        @Test
        void 保存済み名前一覧を返す() {
            var store = new FrameLayoutStore();
            store.save("alpha", createDummySnapshot("a"));
            store.save("beta", createDummySnapshot("b"));

            var names = store.names();

            assertEquals(2, names.size());
            assertTrue(names.contains("alpha"));
            assertTrue(names.contains("beta"));
        }

        @Test
        void 空の場合は空セットを返す() {
            var store = new FrameLayoutStore();

            assertTrue(store.names().isEmpty());
        }
    }

    @Nested
    class remove {

        @Test
        void 存在するスナップショットを削除できる() {
            var store = new FrameLayoutStore();
            store.save("layout1", createDummySnapshot("test"));

            assertTrue(store.remove("layout1"));
            assertFalse(store.load("layout1").isPresent());
        }

        @Test
        void 存在しない名前の削除はfalseを返す() {
            var store = new FrameLayoutStore();

            assertFalse(store.remove("nonexistent"));
        }
    }
}
