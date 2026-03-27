package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UndoManagerTest {

    @Nested
    class 初期状態 {

        @Test
        void undoスタックが空() {
            var manager = new UndoManager();
            assertEquals(0, manager.undoSize());
            assertTrue(manager.undo().isEmpty());
        }

        @Test
        void redoスタックが空() {
            var manager = new UndoManager();
            assertEquals(0, manager.redoSize());
            assertTrue(manager.redo().isEmpty());
        }
    }

    @Nested
    class 記録とundo {

        @Test
        void 記録された逆操作がそのまま返る() {
            var manager = new UndoManager();
            var inverseChange = new TextChange.Delete(0, "Hello");
            manager.record(inverseChange);

            var undoChange = manager.undo().orElseThrow();
            assertEquals(inverseChange, undoChange);
        }

        @Test
        void 複数回の記録は後入れ先出し() {
            var manager = new UndoManager();
            manager.record(new TextChange.Delete(0, "a"));
            manager.record(new TextChange.Delete(1, "b"));

            var change = manager.undo().orElseThrow();
            assertEquals(new TextChange.Delete(1, "b"), change);
        }
    }

    @Nested
    class redo {

        @Test
        void undo後にredoすると逆操作のさらに逆で元の操作が返る() {
            var manager = new UndoManager();
            var inverseChange = new TextChange.Delete(0, "Hello");
            manager.record(inverseChange);
            manager.undo();

            var redoChange = manager.redo().orElseThrow();
            assertEquals(new TextChange.Insert(0, "Hello"), redoChange);
        }

        @Test
        void 通常の編集操作でredoスタックがクリアされる() {
            var manager = new UndoManager();
            manager.record(new TextChange.Delete(0, "a"));
            manager.undo();
            assertEquals(1, manager.redoSize());

            manager.record(new TextChange.Delete(0, "b"));
            assertEquals(0, manager.redoSize());
        }
    }

    @Nested
    class 記録抑制 {

        @Test
        void 抑制中はrecordが無視される() {
            var manager = new UndoManager();
            manager.suppressRecording();
            manager.record(new TextChange.Delete(0, "a"));
            assertEquals(0, manager.undoSize());
        }

        @Test
        void 抑制解除後はrecordが有効になる() {
            var manager = new UndoManager();
            manager.suppressRecording();
            manager.record(new TextChange.Delete(0, "a"));
            manager.resumeRecording();
            manager.record(new TextChange.Delete(0, "b"));
            assertEquals(1, manager.undoSize());
        }
    }
}
