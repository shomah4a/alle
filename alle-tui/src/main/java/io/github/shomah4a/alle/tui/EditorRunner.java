package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.statusline.StatusLineRenderer;
import io.github.shomah4a.alle.core.window.Frame;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 入力・ロジック・描画の3スレッドを管理する。
 * 入力スレッド（呼び出し元）はキー入力の読み取りのみを担当し、
 * ロジックスレッドがコマンド処理・スナップショット作成、
 * 描画スレッドがスナップショットの画面描画を担当する。
 */
public class EditorRunner {

    private final TerminalInputSource inputSource;
    private final Screen screen;
    private final ScreenRenderer renderer;
    private final CommandLoop commandLoop;
    private final Frame frame;
    private final MessageBuffer messageBuffer;
    private final StatusLineRenderer statusLineRenderer;
    private final BlockingQueue<Runnable> actionQueue;

    public EditorRunner(
            TerminalInputSource inputSource,
            Screen screen,
            ScreenRenderer renderer,
            CommandLoop commandLoop,
            Frame frame,
            MessageBuffer messageBuffer,
            StatusLineRenderer statusLineRenderer,
            BlockingQueue<Runnable> actionQueue) {
        this.inputSource = inputSource;
        this.screen = screen;
        this.renderer = renderer;
        this.commandLoop = commandLoop;
        this.frame = frame;
        this.messageBuffer = messageBuffer;
        this.statusLineRenderer = statusLineRenderer;
        this.actionQueue = actionQueue;
    }

    /**
     * 3スレッドでエディタを実行する。
     * 呼び出し元スレッドがキー入力を読み取り、ロジックスレッドに渡す。
     * ロジックスレッドがコマンド処理とスナップショット作成を行い、
     * 描画スレッドがスナップショットを画面に描画する。
     */
    public void run() throws IOException, InterruptedException {
        var keyQueue = new LinkedBlockingQueue<KeyStroke>();
        var exchanger = new SnapshotExchanger();

        var renderThread = new RenderThread(exchanger, screen, renderer);
        var renderThreadHandle = new Thread(renderThread, "editor-render");
        renderThreadHandle.setDaemon(true);
        renderThreadHandle.start();

        var logicThread = new Thread(
                new EditorThread(
                        keyQueue,
                        actionQueue,
                        screen,
                        commandLoop,
                        frame,
                        messageBuffer,
                        exchanger,
                        statusLineRenderer),
                "editor-logic");
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
            exchanger.close();
            renderThreadHandle.join(3000);
        }
    }
}
