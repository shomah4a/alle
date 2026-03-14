package io.github.shomah4a.alle.core.keybind;

import java.util.Optional;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

/**
 * 複数のKeymapを優先順位順に探索し、キーストロークに対応するエントリを解決する。
 * Keymapは先に追加されたものほど優先度が高い。
 */
public class KeyResolver {

    private final MutableList<Keymap> keymaps;

    public KeyResolver() {
        this.keymaps = Lists.mutable.empty();
    }

    /**
     * Keymapを追加する。先に追加されたものほど優先度が高い。
     */
    public void addKeymap(Keymap keymap) {
        keymaps.add(keymap);
    }

    /**
     * キーストロークに対応するエントリを優先順位順に探索して返す。
     */
    public Optional<KeymapEntry> resolve(KeyStroke keyStroke) {
        for (Keymap keymap : keymaps) {
            var entry = keymap.lookup(keyStroke);
            if (entry.isPresent()) {
                return entry;
            }
        }
        return Optional.empty();
    }
}
