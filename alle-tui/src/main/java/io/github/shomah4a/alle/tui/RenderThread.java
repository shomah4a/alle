package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.Loggable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * スナップショットの画面描画を担当する描画スレッド。
 * SnapshotExchangerから最新のスナップショットを受け取り描画する。
 */
class RenderThread implements Runnable, Loggable {

    private final SnapshotExchanger exchanger;
    private final Screen screen;
    private final ScreenRenderer renderer;
    private final AtomicBoolean fullRedrawRequested;

    RenderThread(
            SnapshotExchanger exchanger, Screen screen, ScreenRenderer renderer, AtomicBoolean fullRedrawRequested) {
        this.exchanger = exchanger;
        this.screen = screen;
        this.renderer = renderer;
        this.fullRedrawRequested = fullRedrawRequested;
    }

    /**
     * フル再描画リクエストの有無に応じてリフレッシュタイプを決定する。
     * リクエストがあれば消費してCOMPLETEを返し、なければDELTAを返す。
     */
    static Screen.RefreshType resolveRefreshType(AtomicBoolean fullRedrawRequested) {
        return fullRedrawRequested.compareAndSet(true, false) ? Screen.RefreshType.COMPLETE : Screen.RefreshType.DELTA;
    }

    @Override
    public void run() {
        try {
            while (true) {
                var snapshot = exchanger.awaitNext();
                if (snapshot == null) {
                    break;
                }
                screen.doResizeIfNecessary();
                renderer.renderSnapshot(snapshot);
                screen.refresh(resolveRefreshType(fullRedrawRequested));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger().warn("描画スレッドが割り込みで終了しました");
        } catch (IOException e) {
            logger().error("描画中にエラーが発生しました", e);
        }
    }
}
