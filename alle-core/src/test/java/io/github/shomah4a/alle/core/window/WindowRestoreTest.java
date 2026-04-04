package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.buffer.BufferFacade;
import io.github.shomah4a.alle.core.buffer.TextBuffer;
import io.github.shomah4a.alle.core.setting.SettingsRegistry;
import io.github.shomah4a.alle.core.textmodel.GapTextModel;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WindowRestoreTest {

    private BufferFacade createBuffer(String name, String content) {
        var model = new GapTextModel();
        model.insert(0, content);
        return new BufferFacade(new TextBuffer(name, model, new SettingsRegistry()));
    }

    @Nested
    class restoreFromSnapshot {

        @Test
        void バッファとビュー状態を復元する() {
            var buffer = createBuffer("test.txt", "hello\nworld\nfoo");
            var viewState = new ViewState(5, 1, 0, 0, null);

            var window = Window.restoreFromSnapshot(buffer, viewState, Lists.immutable.empty(), false);

            assertEquals("test.txt", window.getBuffer().getName());
            assertEquals(5, window.getPoint());
            assertEquals(1, window.getDisplayStartLine());
        }

        @Test
        void バッファ履歴を復元する() {
            var buffer = createBuffer("current", "content");
            var historyEntry =
                    new BufferHistoryEntry(new BufferIdentifier.ByName("old"), new ViewState(10, 2, 0, 0, null));

            var window = Window.restoreFromSnapshot(buffer, ViewState.INITIAL, Lists.immutable.of(historyEntry), false);

            assertEquals(1, window.getBufferHistory().size());
            assertEquals(
                    new BufferIdentifier.ByName("old"),
                    window.getBufferHistory().get(0).identifier());
        }

        @Test
        void truncateLinesを復元する() {
            var buffer = createBuffer("test", "content");

            var window = Window.restoreFromSnapshot(buffer, ViewState.INITIAL, Lists.immutable.empty(), true);

            assertTrue(window.isTruncateLines());
        }

        @Test
        void ビュー状態がバッファ範囲超過の場合クランプする() {
            var buffer = createBuffer("short", "ab");
            var viewState = new ViewState(100, 50, 0, 0, null);

            var window = Window.restoreFromSnapshot(buffer, viewState, Lists.immutable.empty(), false);

            assertEquals(2, window.getPoint());
            assertEquals(0, window.getDisplayStartLine());
        }
    }
}
