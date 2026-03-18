package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.Loggable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * スナップショットの画面描画を担当する描画スレッド。
 * ロジックスレッドがAtomicReferenceにスナップショットを格納し通知すると、
 * 最新のスナップショットを取得して描画する。
 */
class RenderThread implements Runnable, Loggable {

    private final AtomicReference<RenderSnapshot> snapshotRef;
    private final Object notifier;
    private final Screen screen;
    private final ScreenRenderer renderer;
    private volatile boolean running = true;

    RenderThread(AtomicReference<RenderSnapshot> snapshotRef, Object notifier, Screen screen, ScreenRenderer renderer) {
        this.snapshotRef = snapshotRef;
        this.notifier = notifier;
        this.screen = screen;
        this.renderer = renderer;
    }

    @Override
    public void run() {
        try {
            while (running) {
                RenderSnapshot snapshot;
                synchronized (notifier) {
                    while (running && snapshotRef.get() == null) {
                        notifier.wait();
                    }
                }
                if (!running) {
                    break;
                }
                snapshot = snapshotRef.getAndSet(null);
                if (snapshot == null) {
                    continue;
                }
                renderer.renderSnapshot(snapshot);
                screen.refresh(Screen.RefreshType.DELTA);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger().warn("描画スレッドが割り込みで終了しました");
        } catch (IOException e) {
            logger().error("描画中にエラーが発生しました", e);
        }
    }

    void requestShutdown() {
        running = false;
        synchronized (notifier) {
            notifier.notifyAll();
        }
    }
}
