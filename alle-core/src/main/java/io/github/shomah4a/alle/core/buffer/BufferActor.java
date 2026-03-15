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
     * 任意の操作をバッファに対して実行する。
     * 将来的にはキュー経由で逐次実行される。
     */
    private <T> CompletableFuture<T> send(Function<Buffer, T> operation) {
        T result = operation.apply(buffer);
        return CompletableFuture.completedFuture(result);
    }

    public CompletableFuture<String> getName() {
        return send(Buffer::getName);
    }

    public CompletableFuture<Optional<Path>> getFilePath() {
        return send(Buffer::getFilePath);
    }

    public CompletableFuture<Void> setFilePath(Path filePath) {
        return send(b -> {
            b.setFilePath(filePath);
            return null;
        });
    }

    public CompletableFuture<Boolean> isDirty() {
        return send(Buffer::isDirty);
    }

    public CompletableFuture<Void> markDirty() {
        return send(b -> {
            b.markDirty();
            return null;
        });
    }

    public CompletableFuture<Void> markClean() {
        return send(b -> {
            b.markClean();
            return null;
        });
    }

    public CompletableFuture<Integer> length() {
        return send(Buffer::length);
    }

    public CompletableFuture<TextChange> insertText(int index, String text) {
        return send(b -> b.insertText(index, text));
    }

    public CompletableFuture<TextChange> deleteText(int index, int count) {
        return send(b -> b.deleteText(index, count));
    }

    public CompletableFuture<TextChange> apply(TextChange change) {
        return send(b -> b.apply(change));
    }

    public CompletableFuture<String> substring(int start, int end) {
        return send(b -> b.substring(start, end));
    }

    public CompletableFuture<Integer> lineCount() {
        return send(Buffer::lineCount);
    }

    public CompletableFuture<Integer> lineIndexForOffset(int offset) {
        return send(b -> b.lineIndexForOffset(offset));
    }

    public CompletableFuture<Integer> lineStartOffset(int lineIndex) {
        return send(b -> b.lineStartOffset(lineIndex));
    }

    public CompletableFuture<String> lineText(int lineIndex) {
        return send(b -> b.lineText(lineIndex));
    }

    public CompletableFuture<String> getText() {
        return send(Buffer::getText);
    }

    /**
     * ラップしているBufferを直接取得する。
     * レンダリング等の同期的なアクセスが必要な場合に使用する。
     */
    public Buffer getBuffer() {
        return buffer;
    }
}
