package io.github.shomah4a.alle.core.keybind;

import io.github.shomah4a.alle.core.command.Command;
import java.util.Optional;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jspecify.annotations.Nullable;

/**
 * キーストロークからKeymapEntryへの対応付け。
 * 純粋なマッピングのみを担当し、優先順位ロジックは持たない。
 */
public class Keymap {

    private static final KeyStroke QUIT_KEY = KeyStroke.ctrl('g');

    private static @Nullable Command quitCommand;

    private final String name;
    private final MutableMap<KeyStroke, KeymapEntry> bindings;
    private @Nullable Command defaultCommand;

    public Keymap(String name) {
        this.name = name;
        this.bindings = Maps.mutable.empty();
    }

    /**
     * 全Keymapインスタンスで C-g に対してフォールバックするquitコマンドを設定する。
     * 明示的にC-gがバインドされているキーマップではそちらが優先される。
     */
    public static void setQuitCommand(Command command) {
        quitCommand = command;
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
     * 指定キーストロークのバインドを解除する。
     */
    public void unbind(KeyStroke keyStroke) {
        bindings.remove(keyStroke);
    }

    /**
     * ASCII印字可能文字(0x20~0x7E)に対して指定コマンドを一括バインドする。
     */
    public void bindPrintableAscii(Command command) {
        for (int codePoint = 0x20; codePoint <= 0x7E; codePoint++) {
            bindings.put(KeyStroke.of(codePoint), new KeymapEntry.CommandBinding(command));
        }
    }

    /**
     * 明示的にバインドされていない印字可能文字に適用するデフォルトコマンドを設定する。
     * 修飾キーなしかつ印字可能なコードポイントの場合にのみ適用される。
     */
    public void setDefaultCommand(Command command) {
        this.defaultCommand = command;
    }

    /**
     * キーストロークに対応するエントリを返す。
     * 明示的バインドがなく、デフォルトコマンドが設定されている場合、
     * 修飾キーなしの印字可能文字にはデフォルトコマンドを返す。
     */
    public Optional<KeymapEntry> lookup(KeyStroke keyStroke) {
        KeymapEntry entry = bindings.get(keyStroke);
        if (entry != null) {
            return Optional.of(entry);
        }
        if (defaultCommand != null && isPrintable(keyStroke)) {
            return Optional.of(new KeymapEntry.CommandBinding(defaultCommand));
        }
        if (quitCommand != null && QUIT_KEY.equals(keyStroke)) {
            return Optional.of(new KeymapEntry.CommandBinding(quitCommand));
        }
        return Optional.empty();
    }

    static boolean isPrintable(KeyStroke keyStroke) {
        if (!keyStroke.modifiers().isEmpty()) {
            return false;
        }
        int codePoint = keyStroke.keyCode();
        return Character.isValidCodePoint(codePoint)
                && Character.getType(codePoint) != Character.CONTROL
                && Character.getType(codePoint) != Character.PRIVATE_USE;
    }
}
