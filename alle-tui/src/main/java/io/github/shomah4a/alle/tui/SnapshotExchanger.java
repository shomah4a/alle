package io.github.shomah4a.alle.tui;

import java.util.concurrent.atomic.AtomicReference;

/**
 * ロジックスレッドから描画スレッドへのスナップショット受け渡しを担う。
 * 最新のスナップショットのみを保持し、中間のスナップショットは上書きされる。
 */
class SnapshotExchanger {

    private final AtomicReference<RenderSnapshot> ref = new AtomicReference<>();

    /**
     * 最新のスナップショットを格納し、描画スレッドに通知する。
     */
    synchronized void publish(RenderSnapshot snapshot) {
        ref.set(snapshot);
        notifyAll();
    }

    /**
     * 新しいスナップショットが利用可能になるまで待機し、取得する。
     * 複数のスナップショットがpublishされていた場合は最新のみを返す。
     *
     * @return スナップショット。shutdownが呼ばれた場合はnull
     */
    synchronized RenderSnapshot awaitNext() throws InterruptedException {
        while (ref.get() == null) {
            wait();
        }
        return ref.getAndSet(null);
    }

    /**
     * 待機中のスレッドを起こす。終了処理用。
     */
    synchronized void wakeUp() {
        notifyAll();
    }
}
