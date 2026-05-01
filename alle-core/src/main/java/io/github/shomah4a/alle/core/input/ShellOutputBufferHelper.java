package io.github.shomah4a.alle.core.input;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.BufferManager;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.styling.FaceName;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;

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

    /**
     * 指定名のバッファを取得または作成し、内容をクリアして返す。
     * バッファが存在しない場合は新規作成して BufferManager に登録する。
     */
    public static BufferFacade getOrCreateAndClear(
            String bufferName, BufferManager bufferManager, SettingsRegistry settingsRegistry) {
        var existing = bufferManager.findByName(bufferName);
        BufferFacade buffer;
        if (existing.isPresent()) {
            buffer = existing.get();
        } else {
            var textBuffer = new TextBuffer(bufferName, new GapTextModel(), settingsRegistry);
            buffer = new BufferFacade(textBuffer);
            buffer.setReadOnly(true);
            bufferManager.add(buffer);
        }
        clearBuffer(buffer);
        return buffer;
    }
}
