package io.github.shomah4a.alle.core.window;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Test;

class WindowTreeSnapshotTest {

    private WindowSnapshot createSnapshot(String bufferName) {
        var current = new BufferHistoryEntry(new BufferIdentifier.ByName(bufferName), ViewState.INITIAL);
        return new WindowSnapshot(current, Lists.immutable.empty(), false);
    }

    @Test
    void Leafはスナップショットを保持する() {
        var snapshot = createSnapshot("*scratch*");
        var leaf = new WindowTreeSnapshot.Leaf(snapshot);

        assertEquals(snapshot, leaf.snapshot());
    }

    @Test
    void Splitは方向と比率と子ノードを保持する() {
        var first = new WindowTreeSnapshot.Leaf(createSnapshot("file1"));
        var second = new WindowTreeSnapshot.Leaf(createSnapshot("file2"));

        var split = new WindowTreeSnapshot.Split(Direction.VERTICAL, 0.6, first, second);

        assertEquals(Direction.VERTICAL, split.direction());
        assertEquals(0.6, split.ratio());
        assertInstanceOf(WindowTreeSnapshot.Leaf.class, split.first());
        assertInstanceOf(WindowTreeSnapshot.Leaf.class, split.second());
    }

    @Test
    void ネストしたSplitで3分割構造を表現できる() {
        var leaf1 = new WindowTreeSnapshot.Leaf(createSnapshot("a"));
        var leaf2 = new WindowTreeSnapshot.Leaf(createSnapshot("b"));
        var leaf3 = new WindowTreeSnapshot.Leaf(createSnapshot("c"));

        var innerSplit = new WindowTreeSnapshot.Split(Direction.HORIZONTAL, 0.5, leaf2, leaf3);
        var outerSplit = new WindowTreeSnapshot.Split(Direction.VERTICAL, 0.6, leaf1, innerSplit);

        assertInstanceOf(WindowTreeSnapshot.Leaf.class, outerSplit.first());
        var inner = assertInstanceOf(WindowTreeSnapshot.Split.class, outerSplit.second());
        assertEquals(Direction.HORIZONTAL, inner.direction());
    }
}
