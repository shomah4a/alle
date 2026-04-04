package io.github.shomah4a.alle.core.window;

/**
 * ウィンドウツリーの構造スナップショット。
 * WindowTreeと同じ木構造を持つが、実体のWindowではなくWindowSnapshotを保持する。
 */
public sealed interface WindowTreeSnapshot {

    /**
     * 単一ウィンドウのスナップショット。
     */
    record Leaf(WindowSnapshot snapshot) implements WindowTreeSnapshot {}

    /**
     * 分割ノードのスナップショット。
     *
     * @param direction 分割方向
     * @param ratio     first側の割合(0.0〜1.0)
     * @param first     1つ目の子(上または左)
     * @param second    2つ目の子(下または右)
     */
    record Split(Direction direction, double ratio, WindowTreeSnapshot first, WindowTreeSnapshot second)
            implements WindowTreeSnapshot {}
}
