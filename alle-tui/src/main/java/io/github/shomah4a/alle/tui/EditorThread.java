package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.window.Frame;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * コマンド処理・スナップショット作成・描画を担当するロジックスレッド。
 * 入力スレッドからキー入力をBlockingQueueで受け取り、
 * 処理結果を画面に描画する。
 */
class EditorThread implements Runnable, Loggable {

    private final BlockingQueue<KeyStroke> keyQueue;
    private final Screen screen;
    private final ScreenRenderer renderer;
    private final CommandLoop commandLoop;
    private final Frame frame;

    EditorThread(
            BlockingQueue<KeyStroke> keyQueue,
            Screen screen,
            ScreenRenderer renderer,
            CommandLoop commandLoop,
            Frame frame) {
        this.keyQueue = keyQueue;
        this.screen = screen;
        this.renderer = renderer;
        this.commandLoop = commandLoop;
        this.frame = frame;
    }

    @Override
    public void run() {
        try {
            // 初期描画
            renderFrame();

            while (true) {
                var keyStroke = keyQueue.take();
                if (keyStroke == POISON_PILL) {
                    break;
                }
                commandLoop.processKey(keyStroke);
                renderFrame();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger().warn("ロジックスレッドが割り込みで終了しました");
        } catch (IOException e) {
            logger().error("描画中にエラーが発生しました", e);
        }
    }

    private void renderFrame() throws IOException {
        var size = screen.getTerminalSize();
        if (size.getRows() < 3) {
            return;
        }
        var snapshot = renderer.createSnapshot(frame, size);
        renderer.renderSnapshot(snapshot);
        screen.refresh(Screen.RefreshType.DELTA);
    }

    /**
     * 終了マーカー。キューにこれを投入するとロジックスレッドが終了する。
     */
    static final KeyStroke POISON_PILL = KeyStroke.of(0xFFFF);
}
