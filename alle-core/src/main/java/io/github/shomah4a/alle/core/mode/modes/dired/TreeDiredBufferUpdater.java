package io.github.shomah4a.alle.core.mode.modes.dired;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.window.Window;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

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
    static final String CUSTOM_COLUMNS_KEY = "dired-custom-columns";

    static final String ROOT_SUFFIX_KEY = "dired-root-suffix";

    static void update(Window window, TreeDiredMode mode) {
        BufferFacade buffer = window.getBuffer();
        TreeDiredModel model = mode.getModel();
        var entries = model.getVisibleEntries();
        var customColumns = resolveCustomColumns(buffer);
        var rootSuffix = resolveRootSuffix(buffer);

        int previousPoint = window.getPoint();

        buffer.atomicOperation(buf -> {
            buf.setReadOnly(false);
            try {
                TreeDiredRenderer.render(
                        buf, model.getRootDirectory(), entries, mode.getZoneId(), customColumns, rootSuffix);
            } finally {
                buf.setReadOnly(true);
            }
            return null;
        });

        // カーソル位置をバッファ範囲内にクランプ
        int newLength = buffer.length();
        window.setPoint(Math.min(previousPoint, newLength));
    }

    static ListIterable<DiredCustomColumn> resolveCustomColumns(BufferFacade buffer) {
        var value = buffer.getVariable(CUSTOM_COLUMNS_KEY).orElse(null);
        if (!(value instanceof ListIterable<?> list)) {
            return Lists.immutable.empty();
        }
        MutableList<DiredCustomColumn> result = Lists.mutable.empty();
        for (Object element : list) {
            if (element instanceof DiredCustomColumn column) {
                result.add(column);
            }
        }
        return result;
    }

    static String resolveRootSuffix(BufferFacade buffer) {
        return buffer.getVariable(ROOT_SUFFIX_KEY)
                .filter(v -> v instanceof String)
                .map(v -> (String) v)
                .orElse("");
    }
}
