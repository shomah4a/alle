package io.github.shomah4a.alle.core.mode;

import io.github.shomah4a.alle.core.highlight.SyntaxHighlighter;
import io.github.shomah4a.alle.core.keybind.Keymap;
import java.util.Optional;

/**
 * Markdownモード。.md/.markdown ファイルに適用される。
 */
public class MarkdownMode implements MajorMode {

    private final MarkdownHighlighter highlighter = new MarkdownHighlighter();

    @Override
    public String name() {
        return "Markdown";
    }

    @Override
    public Optional<Keymap> keymap() {
        return Optional.empty();
    }

    @Override
    public Optional<SyntaxHighlighter> highlighter() {
        return Optional.of(highlighter);
    }
}
