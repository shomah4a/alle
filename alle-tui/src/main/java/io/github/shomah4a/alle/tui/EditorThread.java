package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.window.Frame;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * コマンド処理・スナップショット作成を担当するロジックスレッド。
 * 入力スレッドからキー入力をBlockingQueueで受け取り、
 * 処理結果のスナップショットをAtomicReference経由で描画スレッドに渡す。
 */
class EditorThread implements Runnable, Loggable {

    private final BlockingQueue<KeyStroke> keyQueue;
    private final Screen screen;
    private final ScreenRenderer renderer;
    private final CommandLoop commandLoop;
    private final Frame frame;
    private final AtomicReference<RenderSnapshot> snapshotRef;
    private final Object renderNotifier;

    EditorThread(
            BlockingQueue<KeyStroke> keyQueue,
            Screen screen,
            ScreenRenderer renderer,
            CommandLoop commandLoop,
            Frame frame,
            AtomicReference<RenderSnapshot> snapshotRef,
            Object renderNotifier) {
        this.keyQueue = keyQueue;
        this.screen = screen;
        this.renderer = renderer;
        this.commandLoop = commandLoop;
        this.frame = frame;
        this.snapshotRef = snapshotRef;
        this.renderNotifier = renderNotifier;
    }

    @Override
    public void run() {
        try {
            // 初期描画用スナップショット
            publishSnapshot();

            while (true) {
                var keyStroke = keyQueue.take();
                if (keyStroke == POISON_PILL) {
                    break;
                }
                commandLoop.processKey(keyStroke);
                publishSnapshot();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger().warn("ロジックスレッドが割り込みで終了しました");
        }
    }

    private void publishSnapshot() {
        var size = screen.getTerminalSize();
        if (size.getRows() < 3) {
            return;
        }
        var snapshot = renderer.createSnapshot(frame, size);
        snapshotRef.set(snapshot);
        synchronized (renderNotifier) {
            renderNotifier.notifyAll();
        }
    }

    /**
     * 終了マーカー。キューにこれを投入するとロジックスレッドが終了する。
     */
    static final KeyStroke POISON_PILL = KeyStroke.of(0xFFFF);
}
