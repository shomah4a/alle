package io.github.shomah4a.alle.core.command;

import io.github.shomah4a.alle.core.keybind.Keymap;

/**
 * 何もしないOverridingKeymapController。テスト用。
 */
public class NoOpOverridingKeymapController implements OverridingKeymapController {

    @Override
    public void set(Keymap keymap, Runnable onUnboundKeyExit) {
        // no-op
    }

    @Override
    public void clear() {
        // no-op
    }
}
