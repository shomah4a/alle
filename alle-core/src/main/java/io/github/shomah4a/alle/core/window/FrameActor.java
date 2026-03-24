package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.buffer.Buffer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Frameへの操作をCompletableFutureで返すアクター層。
 * WindowActor/BufferActorと同様に、将来のマルチスレッド化に備える。
 */
public class FrameActor {

    private final Frame frame;

    public FrameActor(Frame frame) {
        this.frame = frame;
    }

    <T> CompletableFuture<T> atomicPerform(Function<Frame, T> operation) {
        try {
            T result = operation.apply(frame);
            return CompletableFuture.completedFuture(result);
        } catch (RuntimeException ex) {
            return CompletableFuture.failedFuture(ex);
        }
    }

    /**
     * ミニバッファが入力受付中かどうかを返す。
     */
    public CompletableFuture<Boolean> isMinibufferActive() {
        return atomicPerform(Frame::isMinibufferActive);
    }

    /**
     * アクティブウィンドウを指定方向に分割し、同一バッファを表示する。
     * アクティブウィンドウは元のウィンドウに留まる。
     */
    public CompletableFuture<Void> splitActiveWindowKeepFocus(Direction direction) {
        return atomicPerform(f -> {
            f.splitActiveWindowKeepFocus(direction);
            return null;
        });
    }

    /**
     * アクティブウィンドウを削除する。
     *
     * @return 削除に成功した場合true
     */
    public CompletableFuture<Boolean> deleteActiveWindow() {
        return atomicPerform(Frame::deleteActiveWindow);
    }

    /**
     * アクティブウィンドウ以外のすべてのウィンドウを閉じる。
     */
    public CompletableFuture<Void> deleteOtherWindows() {
        return atomicPerform(f -> {
            f.deleteOtherWindows();
            return null;
        });
    }

    /**
     * アクティブウィンドウを次のウィンドウに切り替える。
     */
    public CompletableFuture<Void> nextWindow() {
        return atomicPerform(f -> {
            f.nextWindow();
            return null;
        });
    }

    /**
     * 指定バッファを表示中の全ウィンドウを代替バッファに切り替える。
     */
    public CompletableFuture<Void> replaceBufferInAllWindows(Buffer target, Buffer replacement) {
        return atomicPerform(f -> {
            f.replaceBufferInAllWindows(target, replacement);
            return null;
        });
    }

    /**
     * 全ウィンドウの直前バッファが指定バッファの場合にクリアする。
     */
    public CompletableFuture<Void> clearPreviousBufferInAllWindows(Buffer target) {
        return atomicPerform(f -> {
            f.clearPreviousBufferInAllWindows(target);
            return null;
        });
    }

    /**
     * 全ウィンドウで表示中のバッファ名のセットを返す。
     */
    public CompletableFuture<ImmutableSet<String>> getDisplayedBufferNames() {
        return atomicPerform(Frame::getDisplayedBufferNames);
    }

    /**
     * ラップしているFrameを直接取得する。
     * レンダリング等の同期的なアクセスが必要な場合に使用する。
     */
    Frame getFrame() {
        return frame;
    }
}
