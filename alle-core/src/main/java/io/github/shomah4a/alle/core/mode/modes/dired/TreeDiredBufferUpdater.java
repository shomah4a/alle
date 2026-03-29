package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.window.Window;

/**
 * Tree Dired バッファの内容を TreeDiredModel の状態に基づいて更新する。
 * read-only を一時解除し、atomicOperation 内でテキストとfaceを書き換える。
 */
final class TreeDiredBufferUpdater {

    private TreeDiredBufferUpdater() {}

    /**
     * バッファ内容をモデルの現在の状態で更新する。
     * カーソル位置はできる限り維持する。
     */
    static void update(Window window, TreeDiredMode mode) {
        BufferFacade buffer = window.getBuffer();
        TreeDiredModel model = mode.getModel();
        var entries = model.getVisibleEntries();

        int previousPoint = window.getPoint();

        buffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            try {
                TreeDiredRenderer.render(buf, model.getRootDirectory(), entries, mode.getZoneId());
            } finally {
                buf.setReadOnly(true);
            }
            return null;
        });

        // カーソル位置をバッファ範囲内にクランプ
        int newLength = buffer.length();
        window.setPoint(Math.min(previousPoint, newLength));
    }
}
