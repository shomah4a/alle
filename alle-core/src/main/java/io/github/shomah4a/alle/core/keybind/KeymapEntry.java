package io.github.shomah4a.alle.core.keybind;

import io.github.shomah4a.alle.core.command.Command;

/**
 * Keymapのエントリ。コマンドまたは子Keymapのいずれか。
 */
public sealed interface KeymapEntry {

    /**
     * コマンドへのバインド。
     */
    record CommandBinding(Command command) implements KeymapEntry {}

    /**
     * プレフィックスキー用の子Keymap。
     */
    record PrefixBinding(Keymap keymap) implements KeymapEntry {}
}
