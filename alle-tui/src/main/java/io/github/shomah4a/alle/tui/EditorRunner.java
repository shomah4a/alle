package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.window.FrameActor;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 入力・ロジック・スナップショット・描画の4スレッドを管理する。
 * 入力スレッド（呼び出し元）はキー入力の読み取りのみを担当し、
 * ロジックスレッドがコマンド処理、
 * スナップショットスレッドが状態変更トリガーでスナップショット作成、
 * 描画スレッドがスナップショットの画面描画を担当する。
 */
public class EditorRunner {

    private final TerminalInputSource inputSource;
    private final Screen screen;
    private final ScreenRenderer renderer;
    private final CommandLoop commandLoop;
    private final FrameActor frameActor;
    private final BufferManager bufferManager;

    public EditorRunner(
            TerminalInputSource inputSource,
            Screen screen,
            ScreenRenderer renderer,
            CommandLoop commandLoop,
            FrameActor frameActor,
            BufferManager bufferManager) {
        this.inputSource = inputSource;
        this.screen = screen;
        this.renderer = renderer;
        this.commandLoop = commandLoop;
        this.frameActor = frameActor;
        this.bufferManager = bufferManager;
    }

    /**
     * 4スレッドでエディタを実行する。
     * 呼び出し元スレッドがキー入力を読み取り、ロジックスレッドに渡す。
     * ロジックスレッドがコマンド処理を行い、
     * スナップショットスレッドがBufferActor操作完了をトリガーにスナップショットを作成し、
     * 描画スレッドがスナップショットを画面に描画する。
     */
    public void run() throws IOException, InterruptedException {
        var keyQueue = new LinkedBlockingQueue<KeyStroke>();
        var exchanger = new SnapshotExchanger();

        // スナップショットスレッド: BufferActor操作完了をトリガーにスナップショットを作成
        var snapshotThread = new SnapshotThread(screen, renderer, frameActor, exchanger);
        var snapshotThreadHandle = new Thread(snapshotThread, "editor-snapshot");
        snapshotThreadHandle.setDaemon(true);
        snapshotThreadHandle.start();

        // BufferActorの操作完了時にスナップショットのリフレッシュを要求
        bufferManager.setOnActorComplete(snapshotThread::requestRefresh);

        // 描画スレッド: スナップショットを画面に描画
        var renderThread = new RenderThread(exchanger, screen, renderer);
        var renderThreadHandle = new Thread(renderThread, "editor-render");
        renderThreadHandle.setDaemon(true);
        renderThreadHandle.start();

        // ロジックスレッド: キー入力のコマンド処理のみ
        var logicThread =
                new Thread(new EditorThread(keyQueue, commandLoop, snapshotThread::requestRefresh), "editor-logic");
        logicThread.setDaemon(true);
        logicThread.start();

        try {
            // 入力スレッドのメインループ: キーを読んでキューに入れるだけ
            while (true) {
                var keyOpt = inputSource.readKeyStroke();
                if (keyOpt.isEmpty()) {
                    break;
                }
                keyQueue.put(keyOpt.get());
            }
        } finally {
            keyQueue.put(EditorThread.POISON_PILL);
            logicThread.join(3000);
            snapshotThread.close();
            snapshotThreadHandle.join(3000);
            exchanger.close();
            renderThreadHandle.join(3000);
        }
    }
}
