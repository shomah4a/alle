package io.github.shomah4a.alle.core.mode.modes.text;

import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import java.util.Optional;

/**
 * テキストモード。デフォルトのメジャーモード。
 * 特別なキーバインドを持たない。
 */
public class TextMode implements MajorMode {

    @Override
    public String name() {
        return "text";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.empty();
    }
}
