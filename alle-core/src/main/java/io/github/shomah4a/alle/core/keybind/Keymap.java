package io.github.shomah4a.alle.core.keybind;

import io.github.shomah4a.alle.core.command.Command;
import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

/**
 * キーストロークからKeymapEntryへの対応付け。
 * 純粋なマッピングのみを担当し、優先順位ロジックは持たない。
 */
public class Keymap {

    private final String name;
    private final MutableMap<KeyStroke, KeymapEntry> bindings;

    public Keymap(String name) {
        this.name = name;
        this.bindings = Maps.mutable.empty();
    }

    public String getName() {
        return name;
    }

    /**
     * キーストロークにコマンドをバインドする。
     */
    public void bind(KeyStroke keyStroke, Command command) {
        bindings.put(keyStroke, new KeymapEntry.CommandBinding(command));
    }

    /**
     * キーストロークにプレフィックス用の子Keymapをバインドする。
     */
    public void bindPrefix(KeyStroke keyStroke, Keymap prefixKeymap) {
        bindings.put(keyStroke, new KeymapEntry.PrefixBinding(prefixKeymap));
    }

    /**
     * キーストロークに対応するエントリを返す。
     */
    public Optional<KeymapEntry> lookup(KeyStroke keyStroke) {
        return Optional.ofNullable(bindings.get(keyStroke));
    }
}
