package io.github.shomah4a.alle.core.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.shomah4a.alle.core.window.BufferHistoryEntry;
import io.github.shomah4a.alle.core.window.BufferIdentifier;
import io.github.shomah4a.alle.core.window.FrameLayoutStore;
import io.github.shomah4a.alle.core.window.FrameSnapshot;
import io.github.shomah4a.alle.core.window.ViewState;
import io.github.shomah4a.alle.core.window.WindowSnapshot;
import io.github.shomah4a.alle.core.window.WindowTreeSnapshot;
import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class FrameLayoutNameCompleterTest {

    private FrameSnapshot createDummySnapshot() {
        var current = new BufferHistoryEntry(new BufferIdentifier.ByName("test"), ViewState.INITIAL);
        var ws = new WindowSnapshot(current, Lists.immutable.empty(), false);
        return new FrameSnapshot(new WindowTreeSnapshot.Leaf(ws), 0);
    }

    @Test
    void 前方一致で候補を返す() {
        var store = new FrameLayoutStore();
        store.save("work-layout", createDummySnapshot());
        store.save("work-debug", createDummySnapshot());
        store.save("review", createDummySnapshot());

        var completer = new FrameLayoutNameCompleter(store);
        var candidates = completer.complete("work");

        assertEquals(2, candidates.size());
        assertTrue(candidates.anySatisfy(c -> c.value().equals("work-layout")));
        assertTrue(candidates.anySatisfy(c -> c.value().equals("work-debug")));
    }

    @Test
    void 空文字列で全候補を返す() {
        var store = new FrameLayoutStore();
        store.save("alpha", createDummySnapshot());
        store.save("beta", createDummySnapshot());

        var completer = new FrameLayoutNameCompleter(store);
        var candidates = completer.complete("");

        assertEquals(2, candidates.size());
    }

    @Test
    void 一致しない場合空リストを返す() {
        var store = new FrameLayoutStore();
        store.save("layout", createDummySnapshot());

        var completer = new FrameLayoutNameCompleter(store);
        var candidates = completer.complete("xyz");

        assertTrue(candidates.isEmpty());
    }

    @Test
    void 候補はterminalとして返す() {
        var store = new FrameLayoutStore();
        store.save("test", createDummySnapshot());

        var completer = new FrameLayoutNameCompleter(store);
        var candidates = completer.complete("test");

        assertEquals(1, candidates.size());
        assertTrue(candidates.get(0).terminal());
    }
}
