package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.window.WindowActor;
import java.util.concurrent.CompletableFuture;

/**
 * スクリプトに公開するウィンドウ操作のファサード。
 * WindowActor経由で操作し、CompletableFutureを返す。
 * Python側ではJavaFutureラッパーでawait可能。
 */
public class WindowFacade {

    private final WindowActor actor;

    public WindowFacade(WindowActor actor) {
        this.actor = actor;
    }

    /**
     * カーソル位置を返す。
     */
    public CompletableFuture<Integer> point() {
        return actor.getPoint();
    }

    /**
     * カーソルを指定位置に移動する。
     */
    public CompletableFuture<Void> gotoChar(int position) {
        return actor.setPoint(position);
    }

    /**
     * カーソル位置にテキストを挿入する。
     */
    public CompletableFuture<Void> insert(String text) {
        return actor.insert(text);
    }

    /**
     * カーソル位置から前方にcount文字削除する。
     */
    public CompletableFuture<Void> deleteBackward(int count) {
        return actor.deleteBackward(count);
    }

    /**
     * カーソル位置から後方にcount文字削除する。
     */
    public CompletableFuture<Void> deleteForward(int count) {
        return actor.deleteForward(count);
    }

    /**
     * このウィンドウのバッファのファサードを返す。
     */
    public CompletableFuture<BufferFacade> buffer() {
        return actor.getBuffer().thenApply(BufferFacade::new);
    }
}
