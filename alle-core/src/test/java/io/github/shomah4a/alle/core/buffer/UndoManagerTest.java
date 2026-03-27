package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        void withoutRecording内ではrecordが無視される() {
            var manager = new UndoManager();
            manager.withoutRecording(() -> {
                manager.record(new TextChange.Delete(0, "a"));
            });
            assertEquals(0, manager.undoSize());
        }

        @Test
        void withoutRecording終了後はrecordが有効になる() {
            var manager = new UndoManager();
            manager.withoutRecording(() -> {
                manager.record(new TextChange.Delete(0, "a"));
            });
            manager.record(new TextChange.Delete(0, "b"));
            assertEquals(1, manager.undoSize());
        }

        @Test
        void withoutRecording内で例外が発生しても記録状態が復元される() {
            var manager = new UndoManager();
            try {
                manager.withoutRecording(() -> {
                    throw new RuntimeException("test");
                });
            } catch (RuntimeException ignored) {
                // expected
            }
            manager.record(new TextChange.Delete(0, "a"));
            assertEquals(1, manager.undoSize());
        }
    }

    @Nested
    class トランザクション {

        @Test
        void トランザクション内の複数recordが1つのCompoundにまとまる() {
            var manager = new UndoManager();
            manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
                manager.record(new TextChange.Delete(1, "b"));
                manager.record(new TextChange.Delete(2, "c"));
            });

            assertEquals(1, manager.undoSize());
            var change = manager.undo().orElseThrow();
            var compound = assertInstanceOf(TextChange.Compound.class, change);
            assertEquals(3, compound.changes().size());
        }

        @Test
        void トランザクション内でrecordがなければundoスタックに積まれない() {
            var manager = new UndoManager();
            manager.withTransaction(() -> {
                // 何もしない
            });
            assertEquals(0, manager.undoSize());
        }

        @Test
        void トランザクション完了時にredoスタックがクリアされる() {
            var manager = new UndoManager();
            manager.record(new TextChange.Delete(0, "x"));
            manager.undo();
            assertEquals(1, manager.redoSize());

            manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
            });

            assertEquals(0, manager.redoSize());
        }

        @Test
        void ネストされたトランザクションはIllegalStateExceptionをスローする() {
            var manager = new UndoManager();
            assertThrows(IllegalStateException.class, () -> {
                manager.withTransaction(() -> {
                    manager.withTransaction(() -> {
                        // ネスト
                    });
                });
            });
        }

        @Test
        void トランザクション内で例外が発生するとバッファが破棄される() {
            var manager = new UndoManager();
            try {
                manager.withTransaction(() -> {
                    manager.record(new TextChange.Delete(0, "a"));
                    throw new RuntimeException("test");
                });
            } catch (RuntimeException ignored) {
                // expected
            }
            assertEquals(0, manager.undoSize());
        }

        @Test
        void トランザクション内で例外が発生してもトランザクション状態がリセットされる() {
            var manager = new UndoManager();
            try {
                manager.withTransaction(() -> {
                    throw new RuntimeException("test");
                });
            } catch (RuntimeException ignored) {
                // expected
            }
            // 通常のrecordが動作する
            manager.record(new TextChange.Delete(0, "a"));
            assertEquals(1, manager.undoSize());
        }

        @Test
        void Compoundのundo後にredoすると元のCompound操作が復元される() {
            var manager = new UndoManager();
            manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
                manager.record(new TextChange.Delete(1, "b"));
            });

            manager.undo();
            var redoChange = manager.redo().orElseThrow();
            var compound = assertInstanceOf(TextChange.Compound.class, redoChange);
            assertEquals(2, compound.changes().size());
        }
    }
}
