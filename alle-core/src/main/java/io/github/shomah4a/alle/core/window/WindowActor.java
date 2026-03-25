package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.buffer.Buffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Windowへの操作をCompletableFutureで返すアクター層。
 * 内部にキューを持ち、操作を逐次実行する構造を提供する。
 * 現時点では同期的に即時実行し、将来的にキュー+処理スレッドに差し替え可能。
 */
public class WindowActor {

    private final Window window;

    public WindowActor(Window window) {
        this.window = window;
    }

    /**
     * 複数の操作をアトミックに実行する。
     * 将来的にはキュー経由で1つのメッセージとして逐次実行される。
     */
    public <T> CompletableFuture<T> atomicPerform(Function<Window, T> operation) {
        try {
            T result = operation.apply(window);
            return CompletableFuture.completedFuture(result);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    public CompletableFuture<Buffer> getBuffer() {
        return atomicPerform(Window::getBuffer);
    }

    public CompletableFuture<Void> setBuffer(Buffer buffer) {
        return atomicPerform(w -> {
            w.setBuffer(buffer);
            return null;
        });
    }

    public CompletableFuture<Integer> getPoint() {
        return atomicPerform(Window::getPoint);
    }

    public CompletableFuture<Void> setPoint(int point) {
        return atomicPerform(w -> {
            w.setPoint(point);
            return null;
        });
    }

    public CompletableFuture<Integer> getDisplayStartLine() {
        return atomicPerform(Window::getDisplayStartLine);
    }

    public CompletableFuture<Void> setDisplayStartLine(int line) {
        return atomicPerform(w -> {
            w.setDisplayStartLine(line);
            return null;
        });
    }

    public CompletableFuture<Void> insert(String text) {
        return atomicPerform(w -> {
            w.insert(text);
            return null;
        });
    }

    public CompletableFuture<Void> deleteBackward(int count) {
        return atomicPerform(w -> {
            w.deleteBackward(count);
            return null;
        });
    }

    public CompletableFuture<Void> deleteForward(int count) {
        return atomicPerform(w -> {
            w.deleteForward(count);
            return null;
        });
    }

    /**
     * ラップしているWindowを直接取得する。
     * レンダリング等の同期的なアクセスが必要な場合に使用する。
     */
    public Window getWindow() {
        return window;
    }
}
