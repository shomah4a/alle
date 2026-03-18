package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.Loggable;
import java.io.IOException;

/**
 * スナップショットの画面描画を担当する描画スレッド。
 * SnapshotExchangerから最新のスナップショットを受け取り描画する。
 */
class RenderThread implements Runnable, Loggable {

    private final SnapshotExchanger exchanger;
    private final Screen screen;
    private final ScreenRenderer renderer;
    private volatile boolean running = true;

    RenderThread(SnapshotExchanger exchanger, Screen screen, ScreenRenderer renderer) {
        this.exchanger = exchanger;
        this.screen = screen;
        this.renderer = renderer;
    }

    @Override
    public void run() {
        try {
            while (running) {
                var snapshot = exchanger.awaitNext();
                if (snapshot == null || !running) {
                    break;
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
        exchanger.wakeUp();
    }
}
