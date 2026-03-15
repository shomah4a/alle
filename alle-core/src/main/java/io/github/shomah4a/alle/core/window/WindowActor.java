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
     * 任意の操作をウィンドウに対して実行する。
     * 将来的にはキュー経由で逐次実行される。
     */
    private <T> CompletableFuture<T> send(Function<Window, T> operation) {
        T result = operation.apply(window);
        return CompletableFuture.completedFuture(result);
    }

    public CompletableFuture<Buffer> getBuffer() {
        return send(Window::getBuffer);
    }

    public CompletableFuture<Void> setBuffer(Buffer buffer) {
        return send(w -> {
            w.setBuffer(buffer);
            return null;
        });
    }

    public CompletableFuture<Integer> getPoint() {
        return send(Window::getPoint);
    }

    public CompletableFuture<Void> setPoint(int point) {
        return send(w -> {
            w.setPoint(point);
            return null;
        });
    }

    public CompletableFuture<Integer> getDisplayStartLine() {
        return send(Window::getDisplayStartLine);
    }

    public CompletableFuture<Void> setDisplayStartLine(int line) {
        return send(w -> {
            w.setDisplayStartLine(line);
            return null;
        });
    }

    public CompletableFuture<Void> insert(String text) {
        return send(w -> {
            w.insert(text);
            return null;
        });
    }

    public CompletableFuture<Void> deleteBackward(int count) {
        return send(w -> {
            w.deleteBackward(count);
            return null;
        });
    }

    public CompletableFuture<Void> deleteForward(int count) {
        return send(w -> {
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
