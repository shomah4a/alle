package io.github.shomah4a.alle.tui;

import com.googlecode.lanterna.screen.Screen;
import io.github.shomah4a.alle.core.command.CommandLoop;
import io.github.shomah4a.alle.core.keybind.KeyStroke;
import io.github.shomah4a.alle.core.window.Frame;
import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 入力スレッドとロジックスレッドの分離を管理する。
 * 入力スレッド（呼び出し元）はキー入力の読み取りのみを担当し、
 * ロジックスレッドがコマンド処理・スナップショット作成・描画を担当する。
 */
public class EditorRunner {

    private final TerminalInputSource inputSource;
    private final Screen screen;
    private final ScreenRenderer renderer;
    private final CommandLoop commandLoop;
    private final Frame frame;

    public EditorRunner(
            TerminalInputSource inputSource,
            Screen screen,
            ScreenRenderer renderer,
            CommandLoop commandLoop,
            Frame frame) {
        this.inputSource = inputSource;
        this.screen = screen;
        this.renderer = renderer;
        this.commandLoop = commandLoop;
        this.frame = frame;
    }

    /**
     * 入力スレッド + ロジックスレッドでエディタを実行する。
     * 呼び出し元スレッドがキー入力を読み取り、ロジックスレッドに渡す。
     * ロジックスレッドがコマンド処理と描画を行う。
     */
    public void run() throws IOException, InterruptedException {
        var keyQueue = new LinkedBlockingQueue<KeyStroke>();

        var logicThread = new Thread(new EditorThread(keyQueue, screen, renderer, commandLoop, frame), "editor-logic");
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
        }
    }
}
