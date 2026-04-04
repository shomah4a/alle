package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class WindowSnapshotTest {

    @Test
    void currentとhistoryとtruncateLinesを保持する() {
        var current = new BufferHistoryEntry(new BufferIdentifier.ByName("main"), ViewState.INITIAL);
        var historyEntry = new BufferHistoryEntry(
                new BufferIdentifier.ByPath(Path.of("/tmp/old.txt")), new ViewState(10, 5, 0, 0, null));
        var history = Lists.immutable.of(historyEntry);

        var snapshot = new WindowSnapshot(current, history, true);

        assertEquals(current, snapshot.current());
        assertEquals(1, snapshot.history().size());
        assertEquals(historyEntry, snapshot.history().get(0));
        assertTrue(snapshot.truncateLines());
    }

    @Test
    void 空の履歴で生成できる() {
        var current = new BufferHistoryEntry(new BufferIdentifier.ByName("*scratch*"), ViewState.INITIAL);

        var snapshot = new WindowSnapshot(current, Lists.immutable.empty(), false);

        assertEquals(current, snapshot.current());
        assertTrue(snapshot.history().isEmpty());
        assertFalse(snapshot.truncateLines());
    }
}
