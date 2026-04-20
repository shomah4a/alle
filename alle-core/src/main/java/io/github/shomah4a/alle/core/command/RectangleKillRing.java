package io.github.shomah4a.alle.core.command;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;

/**
 * kill-rectangle / copy-rectangle-as-kill で保存された矩形を蓄積するストア。
 * Emacs の killed-rectangle 変数に相当する。
 *
 * <p>矩形は行毎の文字列として保存する。
 * v1 では直近 1 件のみ保持する（put ごとに前のエントリを置き換える）。
 * 将来の履歴拡張余地として Ring 命名を維持する。
 */
public class RectangleKillRing {

    private final MutableList<ImmutableList<String>> entries = Lists.mutable.empty();

    /**
     * 矩形を保存する。現状は直近 1 件のみ保持する。
     */
    public void put(ListIterable<String> lines) {
        entries.clear();
        entries.add(Lists.immutable.withAll(lines));
    }

    /**
     * 直近の矩形を返す。未保存なら empty。
     */
    public Optional<ImmutableList<String>> current() {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(entries.get(0));
    }
}
