package io.github.shomah4a.alle.core.buffer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Bufferへの操作をCompletableFutureで返すアクター層。
 * 内部にキューを持ち、操作を逐次実行する構造を提供する。
 * 現時点では同期的に即時実行し、将来的にキュー+処理スレッドに差し替え可能。
 */
public class BufferActor {

    private final Buffer buffer;

    public BufferActor(Buffer buffer) {
        this.buffer = buffer;
    }

    /**
     * 複数の操作をアトミックに実行する。
     * 将来的にはキュー経由で1つのメッセージとして逐次実行される。
     */
    public <T> CompletableFuture<T> atomicPerform(Function<Buffer, T> operation) {
        T result = operation.apply(buffer);
        return CompletableFuture.completedFuture(result);
    }

    public CompletableFuture<String> getName() {
        return atomicPerform(Buffer::getName);
    }

    public CompletableFuture<Optional<Path>> getFilePath() {
        return atomicPerform(Buffer::getFilePath);
    }

    public CompletableFuture<Void> setFilePath(Path filePath) {
        return atomicPerform(b -> {
            b.setFilePath(filePath);
            return null;
        });
    }

    public CompletableFuture<Boolean> isDirty() {
        return atomicPerform(Buffer::isDirty);
    }

    public CompletableFuture<Void> markDirty() {
        return atomicPerform(b -> {
            b.markDirty();
            return null;
        });
    }

    public CompletableFuture<Void> markClean() {
        return atomicPerform(b -> {
            b.markClean();
            return null;
        });
    }

    public CompletableFuture<Integer> length() {
        return atomicPerform(Buffer::length);
    }

    public CompletableFuture<TextChange> insertText(int index, String text) {
        return atomicPerform(b -> b.insertText(index, text));
    }

    public CompletableFuture<TextChange> deleteText(int index, int count) {
        return atomicPerform(b -> b.deleteText(index, count));
    }

    public CompletableFuture<TextChange> apply(TextChange change) {
        return atomicPerform(b -> b.apply(change));
    }

    public CompletableFuture<String> substring(int start, int end) {
        return atomicPerform(b -> b.substring(start, end));
    }

    public CompletableFuture<Integer> lineCount() {
        return atomicPerform(Buffer::lineCount);
    }

    public CompletableFuture<Integer> lineIndexForOffset(int offset) {
        return atomicPerform(b -> b.lineIndexForOffset(offset));
    }

    public CompletableFuture<Integer> lineStartOffset(int lineIndex) {
        return atomicPerform(b -> b.lineStartOffset(lineIndex));
    }

    public CompletableFuture<String> lineText(int lineIndex) {
        return atomicPerform(b -> b.lineText(lineIndex));
    }

    public CompletableFuture<String> getText() {
        return atomicPerform(Buffer::getText);
    }

    /**
     * ラップしているBufferを直接取得する。
     * レンダリング等の同期的なアクセスが必要な場合に使用する。
     */
    public Buffer getBuffer() {
        return buffer;
    }
}
