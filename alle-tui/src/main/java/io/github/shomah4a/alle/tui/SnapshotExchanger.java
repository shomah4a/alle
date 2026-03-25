package io.github.shomah4a.alle.tui;

import io.github.shomah4a.alle.core.render.RenderSnapshot;
import org.jspecify.annotations.Nullable;

/**
 * ロジックスレッドから描画スレッドへのスナップショット受け渡しを担う。
 * 最新のスナップショットのみを保持し、中間のスナップショットは上書きされる。
 */
class SnapshotExchanger {

    private @Nullable RenderSnapshot pending;
    private boolean closed;

    /**
     * 最新のスナップショットを格納し、描画スレッドに通知する。
     */
    synchronized void publish(RenderSnapshot snapshot) {
        pending = snapshot;
        notifyAll();
    }

    /**
     * 新しいスナップショットが利用可能になるまで待機し、取得する。
     * 複数のスナップショットがpublishされていた場合は最新のみを返す。
     *
     * @return スナップショット。closeされた場合はnull
     */
    synchronized @Nullable RenderSnapshot awaitNext() throws InterruptedException {
        while (pending == null && !closed) {
            wait();
        }
        var snapshot = pending;
        pending = null;
        return snapshot;
    }

    /**
     * エクスチェンジャーを閉じ、待機中のスレッドを起こす。
     */
    synchronized void close() {
        closed = true;
        notifyAll();
    }
}
