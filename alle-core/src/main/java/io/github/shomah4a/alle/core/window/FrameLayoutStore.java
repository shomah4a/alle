package io.github.shomah4a.alle.core.window;

import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * フレームレイアウトの名前付き保存・復元を管理する。
 * Frame の外側に配置し、フレームをまたいでの状態復元を可能にする。
 */
public class FrameLayoutStore {

    private final MutableMap<String, FrameSnapshot> snapshots = Maps.mutable.empty();

    /**
     * フレームスナップショットを名前付きで保存する。
     * 同名のスナップショットが存在する場合は上書きする。
     */
    public void save(String name, FrameSnapshot snapshot) {
        snapshots.put(name, snapshot);
    }

    /**
     * 名前でフレームスナップショットを取得する。
     */
    public Optional<FrameSnapshot> load(String name) {
        return Optional.ofNullable(snapshots.get(name));
    }

    /**
     * 保存済みのスナップショット名一覧を返す。
     */
    public ImmutableSet<String> names() {
        return snapshots.keysView().toImmutableSet();
    }

    /**
     * 指定名のスナップショットを削除する。
     *
     * @return 削除に成功した場合true
     */
    public boolean remove(String name) {
        return snapshots.remove(name) != null;
    }
}
