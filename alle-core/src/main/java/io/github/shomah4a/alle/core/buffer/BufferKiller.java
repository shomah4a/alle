package io.github.shomah4a.alle.core.buffer;

import io.github.shomah4a.alle.core.constants.BufferNames;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import io.github.shomah4a.alle.core.window.BufferIdentifier;
import io.github.shomah4a.alle.core.window.Frame;
import io.github.shomah4a.alle.core.window.Window;
import java.util.Optional;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * バッファの kill 処理を行うユーティリティ。
 * KillBufferCommand と ServerEditCommand で共用する。
 */
public final class BufferKiller {

    private BufferKiller() {}

    /**
     * 指定バッファを kill する。
     * バッファをバッファマネージャから除去し、表示中のウィンドウを代替バッファに切り替え、
     * ウィンドウ履歴から除去する。*scratch* の場合は再作成する。
     *
     * @param bufferManager バッファマネージャ
     * @param frame フレーム
     * @param bufferName kill するバッファ名
     * @param target kill するバッファ
     * @param settingsRegistry 設定レジストリ (*scratch* 再作成時に必要)
     */
    public static void kill(
            BufferManager bufferManager,
            Frame frame,
            String bufferName,
            BufferFacade target,
            SettingsRegistry settingsRegistry) {
        var targetIdentifier = BufferIdentifier.of(target);

        target.getMajorMode().onDisable(target);

        bufferManager.remove(bufferName);

        if (BufferNames.SCRATCH.equals(bufferName)) {
            bufferManager.add(
                    new BufferFacade(new TextBuffer(BufferNames.SCRATCH, new GapTextModel(), settingsRegistry)));
        }

        var allWindows = frame.getWindowTree().windows();
        var fallbackReplacement = findReplacementBuffer(bufferManager, allWindows, target);

        for (var window : allWindows) {
            if (window.getBuffer().equals(target)) {
                var replacement =
                        resolveFirstHistoryBuffer(window, bufferManager, target).orElse(fallbackReplacement);
                window.setBuffer(replacement);
            }
        }

        for (var window : allWindows) {
            window.removeFromBufferHistory(targetIdentifier);
        }
    }

    private static Optional<BufferFacade> resolveFirstHistoryBuffer(
            Window window, BufferManager bufferManager, BufferFacade excluded) {
        for (var entry : window.getBufferHistory()) {
            var found = bufferManager.findByIdentifier(entry.identifier());
            if (found.isPresent() && !found.get().equals(excluded)) {
                return found;
            }
        }
        return Optional.empty();
    }

    private static BufferFacade findReplacementBuffer(
            BufferManager bufferManager, ImmutableList<Window> allWindows, BufferFacade excluded) {
        ImmutableSet<BufferFacade> displayedBuffers =
                allWindows.collect(Window::getBuffer).toSet().toImmutable().newWithout(excluded);

        var candidate = bufferManager.getBuffers().detect(b -> !b.equals(excluded) && !displayedBuffers.contains(b));
        if (candidate != null) {
            return candidate;
        }

        var fallback = bufferManager.getBuffers().detect(b -> !b.equals(excluded));
        if (fallback != null) {
            return fallback;
        }

        throw new IllegalStateException("代替バッファが見つかりません");
    }
}
