package io.github.shomah4a.alle.core.mode.modes.markdown;

import io.github.shomah4a.alle.core.keybind.Keymap;
import io.github.shomah4a.alle.core.mode.MajorMode;
import io.github.shomah4a.alle.core.styling.SyntaxStyler;
import java.util.Optional;

/**
 * Markdownモード。.md/.markdown ファイルに適用される。
 */
public class MarkdownMode implements MajorMode {

    private final MarkdownStyler styler = new MarkdownStyler();

    @Override
    public String name() {
        return "markdown";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.empty();
    }

    @Override
    public Optional<SyntaxStyler> styler() {
        return Optional.of(styler);
    }
}
