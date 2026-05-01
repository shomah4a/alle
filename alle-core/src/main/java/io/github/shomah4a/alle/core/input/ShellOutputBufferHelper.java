package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.styling.FaceName;

/**
 * シェルコマンド出力をバッファに書き込むためのユーティリティ。
 * read-only バッファに対して一時的に read-only を解除して書き込みを行う。
 */
public final class ShellOutputBufferHelper {

    private ShellOutputBufferHelper() {}

    /**
     * バッファ末尾にテキストを追記する。
     */
    public static void appendText(BufferFacade buffer, String text) {
        buffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            buf.insertText(buf.length(), text);
            buf.setReadOnly(true);
            return null;
        });
    }

    /**
     * バッファ末尾にスタイル付きテキストを追記する。
     */
    public static void appendStyledText(BufferFacade buffer, String text, FaceName faceName) {
        buffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            int start = buf.length();
            buf.insertText(start, text);
            buf.putFace(start, start + text.length(), faceName);
            buf.setReadOnly(true);
            return null;
        });
    }

    /**
     * バッファの内容をすべてクリアする。
     */
    public static void clearBuffer(BufferFacade buffer) {
        buffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            int length = buf.length();
            if (length > 0) {
                buf.deleteText(0, length);
            }
            buf.removeFace(0, length);
            buf.setReadOnly(true);
            return null;
        });
    }
}
