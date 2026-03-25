package io.github.shomah4a.alle.tui;

import io.github.shomah4a.alle.core.Loggable;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import java.util.concurrent.BlockingQueue;

/**
 * コマンド処理を担当するロジックスレッド。
 * 入力スレッドからキー入力をBlockingQueueで受け取り、CommandLoopで処理する。
 * スナップショット作成は独立したSnapshotThreadが担当する。
 * processKeyの同期的な状態変更（プレフィックスキー表示等）は
 * refreshCallbackでスナップショットスレッドに通知する。
 */
class EditorThread implements Runnable, Loggable {

    private final BlockingQueue<KeyStroke> keyQueue;
    private final CommandLoop commandLoop;
    private final Runnable refreshCallback;

    EditorThread(BlockingQueue<KeyStroke> keyQueue, CommandLoop commandLoop, Runnable refreshCallback) {
        this.keyQueue = keyQueue;
        this.commandLoop = commandLoop;
        this.refreshCallback = refreshCallback;
    }

    @Override
    public void run() {
        try {
            while (true) {
                var keyStroke = keyQueue.take();
                if (keyStroke.equals(POISON_PILL)) {
                    break;
                }
                var unused = commandLoop.processKey(keyStroke);
                refreshCallback.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger().warn("ロジックスレッドが割り込みで終了しました");
        }
    }

    /**
     * 終了マーカー。キューにこれを投入するとロジックスレッドが終了する。
     */
    static final KeyStroke POISON_PILL = KeyStroke.of(0xFFFF);
}
