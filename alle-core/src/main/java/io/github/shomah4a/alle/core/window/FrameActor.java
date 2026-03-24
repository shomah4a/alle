package io.github.shomah4a.alle.core.window;

import io.github.shomah4a.alle.core.buffer.Buffer;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.keybind.KeyResolver;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.keybind.KeymapEntry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * Frameへの操作をCompletableFutureで返すアクター層。
 * WindowActor/BufferActorと同様に、将来のマルチスレッド化に備える。
 * Window→WindowActorの1:1マッピングを管理し、同一Windowに対して常に同じWindowActorを返す。
 */
public class FrameActor {

    private final Frame frame;
    private final MutableMap<Window, WindowActor> windowActors = Maps.mutable.empty();

    public FrameActor(Frame frame) {
        this.frame = frame;
    }

    /**
     * アクティブウィンドウのWindowActorを返す。
     * 同一Windowに対して常に同じWindowActorインスタンスを返す。
     */
    public WindowActor getActiveWindowActor() {
        var window = frame.getActiveWindow();
        return windowActors.getIfAbsentPut(window, () -> new WindowActor(window));
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

    // ── スナップショット ──

    /**
     * 描画用スナップショットを生成する。
     */
    public CompletableFuture<RenderSnapshot> createSnapshot(int cols, int rows, MessageBuffer messageBuffer) {
        return atomicPerform(f -> f.createSnapshot(cols, rows, messageBuffer));
    }

    // ── キーマップ解決 ──

    /**
     * アクティブウィンドウのバッファのキーマップ情報を含めた4階層でキーストロークを解決する。
     */
    public CompletableFuture<Optional<KeymapEntry>> resolveKey(KeyStroke keyStroke, KeyResolver keyResolver) {
        return atomicPerform(f -> {
            var buffer = f.getActiveWindow().getBuffer();
            var localKeymap = buffer.getLocalKeymap();
            var minorModeKeymaps = collectMinorModeKeymaps(buffer);
            var majorModeKeymap = buffer.getMajorMode().keymap();
            return keyResolver.resolveWithBuffer(keyStroke, localKeymap, minorModeKeymaps, majorModeKeymap);
        });
    }

    private static ListIterable<Keymap> collectMinorModeKeymaps(Buffer buffer) {
        var minorModes = buffer.getMinorModes();
        var result = Lists.mutable.<Keymap>empty();
        for (int i = minorModes.size() - 1; i >= 0; i--) {
            minorModes.get(i).keymap().ifPresent(result::add);
        }
        return result;
    }

    // ── ミニバッファ ──

    /**
     * ミニバッファを入力受付状態にする。
     */
    public CompletableFuture<Void> activateMinibuffer() {
        return atomicPerform(f -> {
            f.activateMinibuffer();
            return null;
        });
    }

    /**
     * ミニバッファの入力受付状態を解除する。
     */
    public CompletableFuture<Void> deactivateMinibuffer() {
        return atomicPerform(f -> {
            f.deactivateMinibuffer();
            return null;
        });
    }

    /**
     * ミニバッファウィンドウのWindowActorを返す。
     */
    public WindowActor getMinibufferWindowActor() {
        var window = frame.getMinibufferWindow();
        return windowActors.getIfAbsentPut(window, () -> new WindowActor(window));
    }

    /**
     * アクティブウィンドウを設定する。WindowActorから内部Windowを解決する。
     */
    public CompletableFuture<Void> setActiveWindow(WindowActor actor) {
        return atomicPerform(f -> {
            // WindowActor のマッピングから Window を逆引き
            var window = windowActors.keysView().detect(w -> windowActors.get(w) == actor);
            if (window != null) {
                f.setActiveWindow(window);
            }
            return null;
        });
    }

    Frame getFrame() {
        return frame;
    }
}
