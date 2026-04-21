package io.github.shomah4a.alle.core.buffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
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
    class クリア {

        @Test
        void clearでundoスタックとredoスタックが空になる() {
            var manager = new UndoManager();
            manager.record(new TextChange.Delete(0, "a"));
            manager.record(new TextChange.Delete(1, "b"));
            manager.undo();

            assertEquals(1, manager.undoSize());
            assertEquals(1, manager.redoSize());

            manager.clear();

            assertEquals(0, manager.undoSize());
            assertEquals(0, manager.redoSize());
            assertTrue(manager.undo().isEmpty());
            assertTrue(manager.redo().isEmpty());
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
                        return CompletableFuture.completedFuture(null);
                    })
                    .join();

            assertEquals(1, manager.undoSize());
            var change = manager.undo().orElseThrow();
            var compound = assertInstanceOf(TextChange.Compound.class, change);
            assertEquals(3, compound.changes().size());
        }

        @Test
        void トランザクション内でrecordがなければundoスタックに積まれない() {
            var manager = new UndoManager();
            manager.withTransaction(() -> CompletableFuture.completedFuture(null))
                    .join();
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
                        return CompletableFuture.completedFuture(null);
                    })
                    .join();

            assertEquals(0, manager.redoSize());
        }

        @Test
        void トランザクション中に呼ばれたトランザクションはキューイングされ別トランザクションになる() {
            var manager = new UndoManager();
            manager.withTransaction(() -> {
                        manager.record(new TextChange.Delete(0, "a"));
                        // トランザクション中に別のトランザクションを開始（キューイングされる）
                        var unused = manager.withTransaction(() -> {
                            manager.record(new TextChange.Delete(1, "b"));
                            return CompletableFuture.completedFuture(null);
                        });
                        return CompletableFuture.completedFuture(null);
                    })
                    .join();

            // 2つの別々のトランザクションとして積まれる
            assertEquals(2, manager.undoSize());
        }

        @Test
        void actionの同期例外でバッファが破棄される() {
            var manager = new UndoManager();
            var future = manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
                throw new RuntimeException("test");
            });
            assertTrue(future.isCompletedExceptionally());
            assertEquals(0, manager.undoSize());
        }

        @Test
        void futureの異常完了でバッファが破棄される() {
            var manager = new UndoManager();
            var future = manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
                return CompletableFuture.failedFuture(new RuntimeException("test"));
            });
            assertTrue(future.isCompletedExceptionally());
            assertEquals(0, manager.undoSize());
        }

        @Test
        void 異常完了後もトランザクション状態がリセットされ通常のrecordが動作する() {
            var manager = new UndoManager();
            var unused = manager.withTransaction(() -> {
                throw new RuntimeException("test");
            });
            manager.record(new TextChange.Delete(0, "a"));
            assertEquals(1, manager.undoSize());
        }

        @Test
        void Compoundのundo後にredoすると元のCompound操作が復元される() {
            var manager = new UndoManager();
            manager.withTransaction(() -> {
                        manager.record(new TextChange.Delete(0, "a"));
                        manager.record(new TextChange.Delete(1, "b"));
                        return CompletableFuture.completedFuture(null);
                    })
                    .join();

            manager.undo();
            var redoChange = manager.redo().orElseThrow();
            var compound = assertInstanceOf(TextChange.Compound.class, redoChange);
            assertEquals(2, compound.changes().size());
        }
    }

    @Nested
    class 非同期トランザクション {

        @Test
        void futureの完了時にコミットされる() {
            var manager = new UndoManager();
            var deferred = new CompletableFuture<Void>();

            var result = manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
                return deferred;
            });

            // future 未完了の間はコミットされていない
            assertEquals(0, manager.undoSize());

            deferred.complete(null);

            assertTrue(result.isDone());
            assertEquals(1, manager.undoSize());
        }

        @Test
        void キューイングにより後続トランザクションは先行完了後に実行される() {
            var manager = new UndoManager();
            var deferred = new CompletableFuture<Void>();

            // T1: 非同期
            var t1 = manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
                return deferred;
            });

            // T2: T1 実行中なのでキューに入る
            var t2 = manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(1, "b"));
                return CompletableFuture.completedFuture(null);
            });

            // T1 未完了、T2 もまだ実行されていない
            assertFalse(t1.isDone());
            assertFalse(t2.isDone());
            assertEquals(0, manager.undoSize());

            // T1 完了 → T1 コミット → T2 実行 → T2 コミット
            deferred.complete(null);

            assertTrue(t1.isDone());
            assertTrue(t2.isDone());
            assertEquals(2, manager.undoSize());
        }

        @Test
        void 先行トランザクション異常完了後も後続トランザクションが実行される() {
            var manager = new UndoManager();
            var deferred = new CompletableFuture<Void>();

            // T1: 異常完了する
            var t1 = manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
                return deferred;
            });

            // T2: キューに入る
            var t2 = manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(1, "b"));
                return CompletableFuture.completedFuture(null);
            });

            // T1 異常完了 → T1 ロールバック → T2 実行 → T2 コミット
            deferred.completeExceptionally(new RuntimeException("fail"));

            assertTrue(t1.isCompletedExceptionally());
            assertTrue(t2.isDone());
            assertEquals(1, manager.undoSize()); // T2 のみ
        }

        @Test
        void 非同期トランザクション中のrecordがバッファリングされる() {
            var manager = new UndoManager();
            var deferred = new CompletableFuture<Void>();

            var result = manager.withTransaction(() -> {
                manager.record(new TextChange.Delete(0, "a"));
                // future 完了前に別スレッドから record される想定
                return deferred.thenRun(() -> {
                    manager.record(new TextChange.Delete(1, "b"));
                });
            });

            assertFalse(result.isDone());
            assertEquals(0, manager.undoSize());

            deferred.complete(null);

            assertTrue(result.isDone());
            assertEquals(1, manager.undoSize());
            var compound =
                    assertInstanceOf(TextChange.Compound.class, manager.undo().orElseThrow());
            assertEquals(2, compound.changes().size());
        }
    }
}
