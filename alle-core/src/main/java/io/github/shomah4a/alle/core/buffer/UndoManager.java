package io.github.shomah4a.alle.core.buffer;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.jspecify.annotations.Nullable;

/**
 * バッファのundo/redo履歴を管理する。
 * テキスト変更の逆操作をスタックに記録し、undo/redoを提供する。
 *
 * <p>トランザクションはキューイングにより直列化される。
 * 先行トランザクションが完了するまで後続のトランザクションは実行されない。
 */
public class UndoManager {

    private final MutableList<TextChange> undoStack;
    private final MutableList<TextChange> redoStack;
    private boolean recording;
    private @Nullable MutableList<TextChange> transactionBuffer;
    private final Queue<PendingTransaction> transactionQueue = new ArrayDeque<>();

    /**
     * キューイング待ちのトランザクションを表す。
     */
    private record PendingTransaction(Supplier<CompletableFuture<Void>> action, CompletableFuture<Void> result) {}

    public UndoManager() {
        this.undoStack = Lists.mutable.empty();
        this.redoStack = Lists.mutable.empty();
        this.recording = true;
    }

    /**
     * テキスト変更を記録する。記録抑制中は何もしない。
     * トランザクション中はバッファに溜め、トランザクション外ではundoスタックに直接積む。
     * 通常の編集操作ではredoスタックをクリアする。
     */
    public synchronized void record(TextChange change) {
        if (!recording) {
            return;
        }
        if (transactionBuffer != null) {
            transactionBuffer.add(change);
        } else {
            undoStack.add(change);
            redoStack.clear();
        }
    }

    /**
     * 最新の変更を取り消す。記録された逆操作を返す。
     * undoスタックが空の場合はemptyを返す。
     */
    public synchronized Optional<TextChange> undo() {
        if (undoStack.isEmpty()) {
            return Optional.empty();
        }
        var change = undoStack.removeLast();
        redoStack.add(change);
        return Optional.of(change);
    }

    /**
     * 直前のundoをやり直す。逆操作のさらに逆操作を返す。
     * redoスタックが空の場合はemptyを返す。
     */
    public synchronized Optional<TextChange> redo() {
        if (redoStack.isEmpty()) {
            return Optional.empty();
        }
        var change = redoStack.removeLast();
        undoStack.add(change);
        return Optional.of(change.inverse());
    }

    /**
     * 記録を抑制した状態でactionを実行する。
     * undo/redo操作の適用時など、バッファ編集がundoスタックに積まれるべきでない場面で使用する。
     * action内で例外が発生しても記録状態は必ず復元される。
     */
    public void withoutRecording(Runnable action) {
        boolean wasRecording = this.recording;
        this.recording = false;
        try {
            action.run();
        } finally {
            this.recording = wasRecording;
        }
    }

    /**
     * 複数のバッファ編集を1つのundo単位にまとめるトランザクション内でactionを実行する。
     * actionは{@link CompletableFuture}を返し、futureの完了時にトランザクションがコミットされる。
     * 同期的なコマンドは{@code CompletableFuture.completedFuture(null)}を返せばよい。
     *
     * <p>トランザクションはキューイングにより直列化される。先行トランザクションの実行中に
     * 呼び出された場合、先行トランザクションの完了後に実行される。
     *
     * <p>action内で発生するrecord()はバッファリングされ、future完了時にCompoundとして
     * 1つのエントリにまとめてundoスタックに積まれる。
     * actionの同期例外またはfutureの異常完了時、バッファリングされた記録は破棄される。
     */
    public CompletableFuture<Void> withTransaction(Supplier<CompletableFuture<Void>> action) {
        var result = new CompletableFuture<Void>();
        synchronized (this) {
            if (transactionBuffer != null) {
                transactionQueue.add(new PendingTransaction(action, result));
                return result;
            }
            transactionBuffer = Lists.mutable.empty();
        }
        executeTransaction(action, result);
        return result;
    }

    private void executeTransaction(Supplier<CompletableFuture<Void>> action, CompletableFuture<Void> result) {
        CompletableFuture<Void> actionFuture;
        try {
            actionFuture = action.get();
        } catch (Exception e) {
            commitOrRollback(e);
            result.completeExceptionally(e);
            drainQueue();
            return;
        }
        var unused = actionFuture.handle((v, ex) -> {
            commitOrRollback(ex);
            if (ex != null) {
                result.completeExceptionally(ex);
            } else {
                result.complete(null);
            }
            drainQueue();
            return null;
        });
    }

    private synchronized void commitOrRollback(@Nullable Throwable ex) {
        if (ex == null && transactionBuffer != null && !transactionBuffer.isEmpty()) {
            var compound =
                    new TextChange.Compound(transactionBuffer.toReversed().toImmutable());
            undoStack.add(compound);
            redoStack.clear();
        }
        transactionBuffer = null;
    }

    private void drainQueue() {
        PendingTransaction next;
        synchronized (this) {
            next = transactionQueue.poll();
            if (next == null) {
                return;
            }
            transactionBuffer = Lists.mutable.empty();
        }
        executeTransaction(next.action(), next.result());
    }

    /**
     * undo/redoスタックをすべてクリアする。
     * バッファの内容がファイルから再読み込みされた場合など、
     * 履歴が無効になった際に使用する。
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    /**
     * undoスタックのサイズを返す。
     */
    public int undoSize() {
        return undoStack.size();
    }

    /**
     * redoスタックのサイズを返す。
     */
    public int redoSize() {
        return redoStack.size();
    }
}
