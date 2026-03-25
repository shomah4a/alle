package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.window.FrameActor;

/**
 * 状態変更通知を受けてスナップショットを作成・公開するスレッド。
 * ロジックスレッドやBufferActorスレッドからの通知をトリガーに、
 * スナップショットを作成してSnapshotExchangerに渡す。
 * 高頻度の通知は間引かれ、最新の1回分のみが処理される。
 */
class SnapshotThread implements Runnable, Loggable {

    private final Screen screen;
    private final ScreenRenderer renderer;
    private final FrameActor frameActor;
    private final SnapshotExchanger exchanger;

    private boolean dirty;
    private boolean closed;

    SnapshotThread(Screen screen, ScreenRenderer renderer, FrameActor frameActor, SnapshotExchanger exchanger) {
        this.screen = screen;
        this.renderer = renderer;
        this.frameActor = frameActor;
        this.exchanger = exchanger;
    }

    /**
     * 状態が変更されたことを通知する。
     * 任意のスレッドから呼び出し可能。
     */
    synchronized void requestRefresh() {
        dirty = true;
        notifyAll();
    }

    /**
     * スナップショットスレッドを停止する。
     */
    synchronized void close() {
        closed = true;
        notifyAll();
    }

    @Override
    public void run() {
        try {
            // 初期描画用スナップショット
            publishSnapshot();

            while (true) {
                awaitDirty();
                if (closed) {
                    break;
                }
                publishSnapshot();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger().warn("スナップショットスレッドが割り込みで終了しました");
        }
    }

    private synchronized void awaitDirty() throws InterruptedException {
        while (!dirty && !closed) {
            wait();
        }
        dirty = false;
    }

    private void publishSnapshot() {
        var size = screen.getTerminalSize();
        if (size.getRows() < 3) {
            return;
        }
        var unused = renderer.createSnapshot(frameActor, size).thenAccept(exchanger::publish);
    }
}
