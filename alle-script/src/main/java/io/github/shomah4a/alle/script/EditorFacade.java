package io.github.shomah4a.alle.script;

import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.MessageBuffer;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.WindowActor;

/**
 * スクリプトに公開するエディタのルートファサード。
 * アクティブウィンドウ・バッファの解決とメッセージ表示を担う。
 * 個別の操作はWindowFacade, BufferFacade等に委譲する。
 *
 * <p>コマンド実行ごとにCommandContextが更新される。
 */
public class EditorFacade {

    private final Frame frame;
    private final BufferManager bufferManager;
    private final MessageBuffer messageBuffer;

    public EditorFacade(Frame frame, BufferManager bufferManager, MessageBuffer messageBuffer) {
        this.frame = frame;
        this.bufferManager = bufferManager;
        this.messageBuffer = messageBuffer;
    }

    /**
     * アクティブウィンドウのファサードを返す。
     */
    public WindowFacade activeWindow() {
        return new WindowFacade(new WindowActor(frame.getActiveWindow()));
    }

    /**
     * アクティブウィンドウのバッファのファサードを返す。
     */
    public BufferFacade currentBuffer() {
        return new BufferFacade(frame.getActiveWindow().getBuffer());
    }

    /**
     * エコーエリアにメッセージを表示する。
     */
    public void message(String text) {
        messageBuffer.message(text);
    }
}
